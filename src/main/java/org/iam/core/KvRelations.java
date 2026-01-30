package org.iam.core;

import org.iam.common.Z3Encoder;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.basetypes.Policy;
import org.iam.common.vars.VarKey;

import java.util.*;

public class KvRelations {
    private Map<VarKey, Map<String, Set<String>>> allRelations;
    private Map<VarKey, Map<String, Set<String>>> allIdoms;

    public KvRelations(Policy<?> policy, EncodedAPI<?> encoder) {
        this.allRelations = new HashMap<>();
        this.allIdoms = new HashMap<>();

        Map<VarKey, Set<String>> kvMaps = policy.getKvMap();
        for (VarKey key : kvMaps.keySet()) {
            kvMaps.get(key).add(
                    switch (key) {
                        case AWS_SOURCE_IP ->  "0.0.0.0/0";
                        default -> "*";
                    }
            );
        }

        for (VarKey key : kvMaps.keySet()) {
            allRelations.put(key, new HashMap<>());
            allIdoms.put(key, new HashMap<>());
            for (String value : kvMaps.get(key)) {
                allRelations.get(key).put(value, new HashSet<>());
                allIdoms.get(key).put(value, new HashSet<>());
            }
            addRelationFromSet(key, kvMaps.getOrDefault(key, Collections.emptySet()), encoder);
        }

        addAllIdoms();
    }

    public final Set<String> idom(VarKey key, String value) {
        return allIdoms.getOrDefault(key, new HashMap<>()).getOrDefault(value, new HashSet<>());
    }

    public final Set<String> contains(VarKey key, String value) {
        return allRelations.getOrDefault(key, new HashMap<>()).getOrDefault(value, new HashSet<>());
    }

    private void addRelationFromSet(VarKey key, Set<String> values, EncodedAPI<?> encoder) {
        if (values == null || values.isEmpty()) {
            this.allRelations.computeIfAbsent(key, k -> new HashMap<>());
            return;
        }

        for (String str1 : values) {
            for (String str2 : values) {
                if (str1.equals(str2)) {
                    continue;
                }

                if (this.allRelations.getOrDefault(key, Collections.emptyMap()).getOrDefault(str1, Collections.emptySet()).contains(str2)
            || this.allRelations.getOrDefault(key, Collections.emptyMap()).getOrDefault(str2, Collections.emptySet()).contains(str1)) {
                    continue;
                }

                if (encoder.greaterThan(key, str1, str2)) {
                    addRelation(key, str1, str2);
                }
            }
        }
    }

    private void addAllIdoms() {
        for (VarKey key : allRelations.keySet()) {
            for (String greater : allRelations.getOrDefault(key, new HashMap<>()).keySet()) {
                Set<String> smallerSet = allRelations.getOrDefault(key, new HashMap<>()).get(greater);
                for (String smaller : smallerSet) {
                    boolean isIdom = true;
                    for (String intermediate : smallerSet) {
                        if (intermediate.equals(smaller)) {
                            continue;
                        }
                        if (allRelations.getOrDefault(key, Collections.emptyMap())
                                .getOrDefault(greater, Collections.emptySet())
                                .contains(intermediate)
                                && allRelations.getOrDefault(key, Collections.emptyMap())
                                .getOrDefault(intermediate, Collections.emptySet())
                                .contains(smaller)) {
                            isIdom = false;
                            break;
                        }
                    }
                    if (isIdom) {
                        addIdom(key, greater, smaller);
                    }
                }
            }
        }
    }

    private void addRelation(VarKey key, String greater, String smaller) {
        this.allRelations.computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(greater, k -> new HashSet<>())
                .add(smaller);
    }

    private void addIdom(VarKey key, String greater, String smaller) {
        this.allIdoms.computeIfAbsent(key, k -> new HashMap<>())
                .computeIfAbsent(greater, k -> new HashSet<>())
                .add(smaller);
    }
}
