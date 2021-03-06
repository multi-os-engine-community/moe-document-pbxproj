/*
Copyright 2014-2016 Intel Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.moe.document.pbxproj;

import org.moe.document.pbxproj.nextstep.Dictionary;
import org.moe.document.pbxproj.nextstep.NextStep;
import org.moe.document.pbxproj.nextstep.Value;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unchecked")
public class Root extends PBXObject {

    public static final String ARCHIVE_VERSION_KEY = "archiveVersion";
    public static final String CLASSES_KEY = "classes";
    public static final String OBJECT_VERSION_KEY = "objectVersion";
    public static final String OBJECTS_KEY = "objects";
    public static final String ROOT_OBJECT_KEY = "rootObject";

    public Root(Dictionary<Value, NextStep> dict) {
        super(dict);
    }

    public Root() {
        super(null);
        setIsa(null);
    }

    @Override
    public String print(int depth) {
        return "// !$*UTF8*$!\n" + super.print(0) + "\n";
    }

    @Override
    public String printInline(int depth) {
        throw new RuntimeException("root's printInline method should never be called");
    }

    @Override
    public String getCommentString() {
        return null;
    }

    @Override
    public void connectReferences(Map<String, Value> map) {
        connectReferencesInValue(ROOT_OBJECT_KEY, map);
    }

    @Override
    public void removeReference(PBXObjectRef<? extends PBXObject> ref) {
        if (ref.equals(getRootObject())) {
            setRootObject(null);
        }
    }

    /*
     * Fields
     */

    public String getArchiveVersion() {
        return getStringValue(ARCHIVE_VERSION_KEY);
    }

    public void setArchiveVersion(String value) {
        setStringValue(ARCHIVE_VERSION_KEY, value);
    }

    public Dictionary<Value, NextStep> getClasses() {
        return getOrCreateDictionaryValue(CLASSES_KEY);
    }

    public String getObjectVersion() {
        return getStringValue(OBJECT_VERSION_KEY);
    }

    public void setObjectVersion(String value) {
        setStringValue(OBJECT_VERSION_KEY, value);
    }

    private RootObjects objects;

    public RootObjects getObjects() {
        if (objects == null) {
            objects = new RootObjects(
                    this.<PBXObjectRef<? extends PBXObject>, PBXObject>getOrCreateDictionaryValue(OBJECTS_KEY));
        }
        return objects;
    }

    public PBXObjectRef<PBXProject> getRootObject() {
        return getPBXObjectRefValue(ROOT_OBJECT_KEY);
    }

    public void setRootObject(PBXObjectRef<PBXProject> value) {
        setPBXObjectRefValue(ROOT_OBJECT_KEY, value);
    }

    public List<PBXGroup> getGroups() {
        List<PBXGroup> result = new ArrayList<PBXGroup>();

        for (Dictionary.Field<PBXObjectRef<? extends PBXObject>, PBXObject> field : getObjects().rawData()) {
            if (field.getValue() instanceof PBXGroup) {
                result.add((PBXGroup)field.getValue());
            }
        }

        return result;
    }

    public List<PBXObjectRef<PBXFileReference>> getFileReferences() {
        List<PBXObjectRef<PBXFileReference>> result = new ArrayList<PBXObjectRef<PBXFileReference>>();

        for (Dictionary.Field<PBXObjectRef<? extends PBXObject>, PBXObject> field : getObjects().rawData()) {
            if (field.getValue() instanceof PBXFileReference) {
                result.add((PBXObjectRef<PBXFileReference>)field.getKey());
            }
        }

        return result;
    }

    public List<PBXObjectRef<PBXBuildPhase>> getBuildPhaseReferences() {
        List<PBXObjectRef<PBXBuildPhase>> result = new ArrayList<PBXObjectRef<PBXBuildPhase>>();

        for (Dictionary.Field<PBXObjectRef<? extends PBXObject>, PBXObject> field : getObjects().rawData()) {
            if (field.getValue() instanceof PBXBuildPhase) {
                result.add((PBXObjectRef<PBXBuildPhase>)field.getKey());
            }
        }

        return result;
    }

    public List<PBXObjectRef<PBXBuildFile>> getBuildFileReferences() {
        List<PBXObjectRef<PBXBuildFile>> result = new ArrayList<PBXObjectRef<PBXBuildFile>>();

        for (Dictionary.Field<PBXObjectRef<? extends PBXObject>, PBXObject> field : getObjects().rawData()) {
            if (field.getValue() instanceof PBXBuildFile) {
                result.add((PBXObjectRef<PBXBuildFile>)field.getKey());
            }
        }

        return result;
    }

    /*
     * Helper classes
     */

    public static class RootObjects extends Dictionary<PBXObjectRef<? extends PBXObject>, PBXObject> {

        RootObjects(Dictionary<PBXObjectRef<? extends PBXObject>, PBXObject> dict) {
            super(dict, false);
        }

        public void sortObjects() {
            Collections.sort(rawData(), new Comparator<Field<PBXObjectRef<?>, PBXObject>>() {
                @Override
                public int compare(Field<PBXObjectRef<?>, PBXObject> o1, Field<PBXObjectRef<?>, PBXObject> o2) {
                    String isa1 = o1.getValue().getIsa();
                    String isa2 = o2.getValue().getIsa();
                    if (isa1 == null && isa2 == null) {
                        return 0;
                    } else if (isa1 == null) {
                        return -1;
                    } else if (isa2 == null) {
                        return 1;
                    }
                    final int isa = isa1.compareTo(isa2);
                    if (isa != 0) {
                        return isa;
                    }
                    return o1.getKey().value.compareTo(o2.getKey().value);
                }
            });
        }

        public void updateObjects() {
            for (Entry<PBXObjectRef<? extends PBXObject>, PBXObject> entry : entrySet()) {
                entry.getValue().update();
            }
        }

        public void put(PBXObjectRef<? extends PBXObject> ref) {
            put(ref, ref.getReferenced());
        }

        public PBXObject remove(Object ref) {
            PBXObject removed = super.remove(ref);
            if (removed != null) {
                for (Entry<PBXObjectRef<? extends PBXObject>, PBXObject> entry : entrySet()) {
                    entry.getValue().removeReference((PBXObjectRef<? extends PBXObject>)ref);
                }
            }
            return removed;
        }

        public PBXObject removeRaw(final PBXObjectRef<? extends PBXObject> ref) {
            return super.remove(ref);
        }

        public void replaceKey(Entry<Value, NextStep> field, PBXObjectRef<PBXObject> key) {
            EntryImpl entry = (EntryImpl)field;
            entry.replaceKey(key);
        }
    }

}
