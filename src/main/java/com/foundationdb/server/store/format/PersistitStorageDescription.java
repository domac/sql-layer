/**
 * Copyright (C) 2009-2013 FoundationDB, LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.foundationdb.server.store.format;

import com.foundationdb.ais.model.HasStorage;
import com.foundationdb.ais.model.StorageDescription;
import com.foundationdb.ais.model.validation.AISValidationFailure;
import com.foundationdb.ais.model.validation.AISValidationOutput;
import com.foundationdb.ais.protobuf.AISProtobuf.Storage;
import com.foundationdb.ais.protobuf.PersistitProtobuf;
import com.foundationdb.server.error.StorageDescriptionInvalidException;
import com.foundationdb.server.service.tree.TreeCache;
import com.foundationdb.server.service.tree.TreeLink;

/** Storage in a persistit volume tree. 
 * Goes to a lot of trouble to arrange for the name of the tree to be
 * meaningful, while still unique.
*/
public class PersistitStorageDescription extends StorageDescription implements TreeLink
{
    private String treeName;
    private TreeCache treeCache;

    public PersistitStorageDescription(HasStorage forObject) {
        super(forObject);
    }

    public PersistitStorageDescription(HasStorage forObject, String treeName) {
        super(forObject);
        this.treeName = treeName;
    }

    public PersistitStorageDescription(HasStorage forObject, PersistitStorageDescription other) {
        super(forObject);
        this.treeName = other.treeName;
    }

    @Override
    public StorageDescription cloneForObject(HasStorage forObject) {
        return new PersistitStorageDescription(forObject, this);
    }

    @Override
    public void writeProtobuf(Storage.Builder builder) {
        builder.setExtension(PersistitProtobuf.treeName, treeName);
    }

    @Override
    public String getTreeName() {
        return treeName;
    }

    protected void setTreeName(String treeName) {
        this.treeName = treeName;
    }

    @Override
    public TreeCache getTreeCache() {
        return treeCache;
    }

    @Override
    public void setTreeCache(TreeCache treeCache) {
        this.treeCache = treeCache;
    }

    @Override
    public Object getUniqueKey() {
        return treeName;
    }

    @Override
    public String getNameString() {
        return treeName;
    }

    @Override
    public void validate(AISValidationOutput output) {
        if (treeName == null) {
            output.reportFailure(new AISValidationFailure(new StorageDescriptionInvalidException(object, "is missing tree name")));
        }
    }

}