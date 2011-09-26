/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.test.it.qp;

import com.akiban.ais.model.*;
import com.akiban.qp.exec.UpdatePlannable;
import com.akiban.qp.exec.UpdateResult;
import com.akiban.qp.expression.Expression;
import com.akiban.qp.expression.IndexBound;
import com.akiban.qp.expression.IndexKeyRange;
import com.akiban.qp.operator.Bindings;
import com.akiban.qp.operator.Cursor;
import com.akiban.qp.operator.Operator;
import com.akiban.qp.operator.UpdateFunction;
import com.akiban.qp.row.OverlayingRow;
import com.akiban.qp.row.Row;
import com.akiban.qp.row.RowBase;
import com.akiban.qp.rowtype.RowType;
import com.akiban.server.types.AkType;
import com.akiban.server.types.conversion.Converters;
import com.akiban.server.types.ToObjectValueTarget;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.akiban.qp.expression.API.*;
import static com.akiban.qp.operator.API.*;
import static com.akiban.qp.operator.API.JoinType.*;
import static org.junit.Assert.assertEquals;

public class OperatorIT extends OperatorITBase
{
    @Before
    public void before()
    {
        super.before();
        use(db);
    }

    @Test
    public void basicUpdate() throws Exception {
        adapter.setTransactional(false);

        UpdateFunction updateFunction = new UpdateFunction() {
            @Override
            public boolean rowIsSelected(Row row) {
                return row.rowType().equals(customerRowType);
            }

            @Override
            public Row evaluate(Row original, Bindings bindings) {
                ToObjectValueTarget target = new ToObjectValueTarget();
                target.expectType(AkType.VARCHAR);
                Object obj = Converters.convert(original.eval(1), target).lastConvertedValue();
                String name = (String) obj; // TODO eventually use Expression for this
                name = name.toUpperCase();
                name = name + name;
                return new OverlayingRow(original).overlay(1, name);
            }
        };

        Operator groupScan = groupScan_Default(coi);
        UpdatePlannable updateOperator = update_Default(groupScan, updateFunction);
        UpdateResult result = updateOperator.run(NO_BINDINGS, adapter);
        assertEquals("rows modified", 2, result.rowsModified());
        assertEquals("rows touched", db.length, result.rowsTouched());

        Cursor executable = cursor(groupScan, adapter);
        RowBase[] expected = new RowBase[]{row(customerRowType, 1L, "XYZXYZ"),
                                           row(orderRowType, 11L, 1L, "ori"),
                                           row(itemRowType, 111L, 11L),
                                           row(itemRowType, 112L, 11L),
                                           row(orderRowType, 12L, 1L, "david"),
                                           row(itemRowType, 121L, 12L),
                                           row(itemRowType, 122L, 12L),
                                           row(customerRowType, 2L, "ABCABC"),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(itemRowType, 222L, 22L)
        };
        compareRows(expected, executable);
    }

    @Test
    public void testGroupScan() throws Exception
    {
        Operator groupScan = groupScan_Default(coi);
        Cursor executable = cursor(groupScan, adapter);
        RowBase[] expected = new RowBase[]{row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 11L, 1L, "ori"),
                                           row(itemRowType, 111L, 11L),
                                           row(itemRowType, 112L, 11L),
                                           row(orderRowType, 12L, 1L, "david"),
                                           row(itemRowType, 121L, 12L),
                                           row(itemRowType, 122L, 12L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(itemRowType, 222L, 22L)
        };
        compareRows(expected, executable);
    }

    @Test
    public void testSelect()
    {
        Operator groupScan = groupScan_Default(coi);
        Expression cidEq2 = compare(field(0), EQ, literal(2L));
        Operator select = select_HKeyOrdered(groupScan, customerRowType, cidEq2);
        RowBase[] expected = new RowBase[]{row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(itemRowType, 222L, 22L)};
        compareRows(expected, cursor(select, adapter));
    }

    @Test
    public void testFlatten()
    {
        Operator groupScan = groupScan_Default(coi);
        Operator flatten = flatten_HKeyOrdered(groupScan, customerRowType, orderRowType, INNER_JOIN);
        RowType flattenType = flatten.rowType();
        RowBase[] expected = new RowBase[]{row(flattenType, 1L, "xyz", 11L, 1L, "ori"),
                                           row(itemRowType, 111L, 11L),
                                           row(itemRowType, 112L, 11L),
                                           row(flattenType, 1L, "xyz", 12L, 1L, "david"),
                                           row(itemRowType, 121L, 12L),
                                           row(itemRowType, 122L, 12L),
                                           row(flattenType, 2L, "abc", 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L),
                                           row(flattenType, 2L, "abc", 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(itemRowType, 222L, 22L)};
        compareRows(expected, cursor(flatten, adapter));
    }

    @Test
    public void testTwoFlattens()
    {
        Operator groupScan = groupScan_Default(coi);
        Operator flattenCO = flatten_HKeyOrdered(groupScan, customerRowType, orderRowType, INNER_JOIN);
        Operator flattenCOI = flatten_HKeyOrdered(flattenCO, flattenCO.rowType(), itemRowType, INNER_JOIN);
        RowType flattenCOIType = flattenCOI.rowType();
        RowBase[] expected = new RowBase[]{row(flattenCOIType, 1L, "xyz", 11L, 1L, "ori", 111L, 11L),
                                           row(flattenCOIType, 1L, "xyz", 11L, 1L, "ori", 112L, 11L),
                                           row(flattenCOIType, 1L, "xyz", 12L, 1L, "david", 121L, 12L),
                                           row(flattenCOIType, 1L, "xyz", 12L, 1L, "david", 122L, 12L),
                                           row(flattenCOIType, 2L, "abc", 21L, 2L, "tom", 211L, 21L),
                                           row(flattenCOIType, 2L, "abc", 21L, 2L, "tom", 212L, 21L),
                                           row(flattenCOIType, 2L, "abc", 22L, 2L, "jack", 221L, 22L),
                                           row(flattenCOIType, 2L, "abc", 22L, 2L, "jack", 222L, 22L)};
        compareRows(expected, cursor(flattenCOI, adapter));
    }

    @Test
    public void testIndexScan1()
    {
        Operator indexScan = indexScan_Default(indexType(customer, "name"));
        // TODO: Can't compare rows, because we can't yet obtain fields from index rows. So compare hkeys instead
        String[] expected = new String[]{"{1,(long)2}",
                                         "{1,(long)1}"};
        compareRenderedHKeys(expected, cursor(indexScan, adapter));
    }

    @Test
    public void testIndexScan2()
    {
        Operator indexScan = indexScan_Default(indexType(order, "salesman"));
        // TODO: Can't compare rows, because we can't yet obtain fields from index rows. So compare hkeys instead
        String[] expected = new String[]{"{1,(long)1,2,(long)12}",
                                         "{1,(long)2,2,(long)22}",
                                         "{1,(long)1,2,(long)11}",
                                         "{1,(long)2,2,(long)21}"};
        compareRenderedHKeys(expected, cursor(indexScan, adapter));
    }

    @Test
    public void testIndexLookup()
    {
        Operator indexScan = indexScan_Default(indexType(order, "salesman"));
        Operator lookup = branchLookup_Default(indexScan, coi, orderSalesmanIndexRowType, orderRowType, LookupOption.DISCARD_INPUT);
        RowBase[] expected = new RowBase[]{row(orderRowType, 12L, 1L, "david"),
                                           row(itemRowType, 121L, 12L),
                                           row(itemRowType, 122L, 12L),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(itemRowType, 222L, 22L),
                                           row(orderRowType, 11L, 1L, "ori"),
                                           row(itemRowType, 111L, 11L),
                                           row(itemRowType, 112L, 11L),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L)};
        compareRows(expected, cursor(lookup, adapter));
    }

    @Test
    public void testIndexLookupWithOneAncestor()
    {
        Operator indexScan = indexScan_Default(indexType(order, "salesman"));
        Operator lookup = branchLookup_Default(indexScan, coi, orderSalesmanIndexRowType, orderRowType, LookupOption.DISCARD_INPUT);
        Operator ancestorLookup = ancestorLookup_Default(lookup,
                                                                 coi,
                                                                 orderRowType,
                                                                 Arrays.asList(customerRowType),
                                                                 LookupOption.KEEP_INPUT);
        RowBase[] expected = new RowBase[]{row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 12L, 1L, "david"),
                                           row(itemRowType, 121L, 12L),
                                           row(itemRowType, 122L, 12L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(itemRowType, 222L, 22L),
                                           row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 11L, 1L, "ori"),
                                           row(itemRowType, 111L, 11L),
                                           row(itemRowType, 112L, 11L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L)};
        compareRows(expected, cursor(ancestorLookup, adapter));
    }

    @Test
    public void testIndexLookupWithTwoAncestors()
    {
        Operator indexScan = indexScan_Default(indexType(item, "oid"));
        Operator lookup = branchLookup_Default(indexScan,
                                                       coi,
                                                       itemOidIndexRowType,
                                                       itemRowType,
                                                       LookupOption.DISCARD_INPUT);
        Operator ancestorLookup = ancestorLookup_Default(lookup,
                                                                 coi,
                                                                 itemRowType,
                                                                 Arrays.asList(customerRowType, orderRowType),
                                                                 LookupOption.KEEP_INPUT);
        RowBase[] expected = new RowBase[]{row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 11L, 1L, "ori"),
                                           row(itemRowType, 111L, 11L),
                                           row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 11L, 1L, "ori"),
                                           row(itemRowType, 112L, 11L),
                                           row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 12L, 1L, "david"),
                                           row(itemRowType, 121L, 12L),
                                           row(customerRowType, 1L, "xyz"),
                                           row(orderRowType, 12L, 1L, "david"),
                                           row(itemRowType, 122L, 12L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 212L, 21L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 221L, 22L),
                                           row(customerRowType, 2L, "abc"),
                                           row(orderRowType, 22L, 2L, "jack"),
                                           row(itemRowType, 222L, 22L)};
        compareRows(expected, cursor(ancestorLookup, adapter));
    }

    @Test
    public void testRestrictedIndexScan()
    {
        Index idxOrderSalesman = orderSalesmanIndexRowType.index();
        IndexBound lo = indexBound(row(orderSalesmanIndexRowType, "jack"), columnSelector(idxOrderSalesman));
        IndexBound hi = indexBound(row(orderSalesmanIndexRowType, "tom"), columnSelector(idxOrderSalesman));
        IndexKeyRange range = indexKeyRange(lo, true, hi, false);
        Operator indexScan = indexScan_Default(orderSalesmanIndexRowType, false, range);
        // TODO: Can't compare rows, because we can't yet obtain fields from index rows. So compare hkeys instead
        String[] expected = new String[]{"{1,(long)2,2,(long)22}",
                                         "{1,(long)1,2,(long)11}"};
        compareRenderedHKeys(expected, cursor(indexScan, adapter));
    }

    @Test
    public void testRestrictedIndexLookup()
    {
        Index idxOrderSalesman = orderSalesmanIndexRowType.index();
        IndexBound tom = indexBound(row(orderSalesmanIndexRowType, "tom"), columnSelector(idxOrderSalesman));
        IndexKeyRange matchTom = indexKeyRange(tom, true, tom, true);
        Operator indexScan = indexScan_Default(orderSalesmanIndexRowType, false, matchTom);
        Operator lookup = branchLookup_Default(indexScan,
                                                       coi,
                                                       orderSalesmanIndexRowType,
                                                       orderRowType,
                                                       LookupOption.DISCARD_INPUT);
        RowBase[] expected = new RowBase[]{row(orderRowType, 21L, 2L, "tom"),
                                           row(itemRowType, 211L, 21L),
                                           row(itemRowType, 212L, 21L)};
        compareRows(expected, cursor(lookup, adapter));

    }

    @Test
    public void testAncestorLookupAfterIndexScan()
    {
        // Find customers associated with salesman tom
        Index idxOrderSalesman = orderSalesmanIndexRowType.index();
        IndexBound tom = indexBound(row(orderSalesmanIndexRowType, "tom"), columnSelector(idxOrderSalesman));
        IndexKeyRange matchTom = indexKeyRange(tom, true, tom, true);
        Operator indexScan = indexScan_Default(orderSalesmanIndexRowType, false, matchTom);
        Operator ancestorLookup = ancestorLookup_Default(indexScan,
                                                                 coi,
                                                                 orderSalesmanIndexRowType,
                                                                 Arrays.asList(customerRowType),
                                                                 LookupOption.DISCARD_INPUT);
        RowBase[] expected = new RowBase[]{row(customerRowType, 2L, "abc")};
        compareRows(expected, cursor(ancestorLookup, adapter));
    }
}