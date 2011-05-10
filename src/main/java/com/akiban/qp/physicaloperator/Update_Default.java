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

package com.akiban.qp.physicaloperator;

import com.akiban.qp.row.Row;
import com.akiban.util.ArgumentValidation;

import java.util.Collections;
import java.util.List;

public final class Update_Default extends PhysicalOperator {

    // Object interface

    @Override
    public String toString() {
        return String.format("%s(%s -> %s)", getClass().getSimpleName(), inputOperator, updateLambda);
    }

    // constructor

    public Update_Default(PhysicalOperator inputOperator, UpdateLambda updateLambda) {
        ArgumentValidation.notNull("update lambda", updateLambda);
        if (!inputOperator.cursorAbilitiesInclude(CursorAbility.MODIFY)) {
            throw new IllegalArgumentException("input operator must be modifiable: " + inputOperator.getClass());
        }
        
        this.inputOperator = inputOperator;
        this.updateLambda = updateLambda;
    }

    // PhysicalOperator interface

    @Override
    public Cursor cursor(StoreAdapter adapter) {
        Cursor inputCursor = inputOperator.cursor(adapter);
        return new Execution(inputCursor, updateLambda);
    }

    @Override
    public List<PhysicalOperator> getInputOperators() {
        return Collections.singletonList(inputOperator);
    }

    @Override
    public String describePlan()
    {
        return describePlan(inputOperator);
    }

    // Object state

    private final PhysicalOperator inputOperator;
    private final UpdateLambda updateLambda;

    // Inner classes

    private class Execution extends ChainedCursor {

        private final UpdateLambda updateLambda;
        private Bindings bindings;

        public Execution(Cursor input, UpdateLambda updateLambda) {
            super(input);
            this.updateLambda = updateLambda;
            this.bindings = UndefBindings.only();
        }

        // Cursor interface


        @Override
        public void open(Bindings bindings) {
            super.open(bindings);
            this.bindings = bindings;
        }

        @Override
        public boolean next() {
            if (input.next()) {
                Row row = this.input.currentRow();
                if (!updateLambda.rowIsApplicable(row)) {
                    return true;
                }
                Row currentRow = updateLambda.applyUpdate(row, bindings);
                input.updateCurrentRow(currentRow);
                return true;
            }
            return false;
        }
    }

    
}
