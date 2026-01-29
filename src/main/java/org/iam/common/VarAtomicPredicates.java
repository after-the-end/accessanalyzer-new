package org.iam.common;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import org.iam.common.apis.EncodedAPI;
import org.iam.common.reduce.DynamicVar;
import org.iam.common.reduce.StaticVar;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class VarAtomicPredicates<T> {
    private final Set<StaticVar<T>> _staticVars;

    private int _numAtomicPredicates;

    private Map<StaticVar<T>, Set<Integer>> _varToPredicates;

//    private Map<Integer, DynamicVar<T>> _predicatesToVar;

    public VarAtomicPredicates(Set<StaticVar<T>> staticVars, StaticVar<T> trueStaticVar, EncodedAPI<T> encoder) {
        _staticVars = ImmutableSet.<StaticVar<T>>builder()
                .addAll(staticVars)
                .add(trueStaticVar)
                .build();
        initAtomicPredicates(encoder);
    }

    private void initAtomicPredicates(EncodedAPI<T> encoder) {
        SetMultimap<DynamicVar<T>, StaticVar<T>> mmap = HashMultimap.create();
        for (StaticVar<T> textElement : _staticVars) {
            DynamicVar<T> restDynamicVar = textElement.convert(encoder);
            if (restDynamicVar.isEmpty()) {
                throw new RuntimeException("StaticVar " + textElement + " is empty (UNSAT)");
            }
            SetMultimap<DynamicVar<T>, StaticVar<T>> newMMap = HashMultimap.create(mmap);
            for (DynamicVar<T> dynamicVar : mmap.keySet()) {
                if (dynamicVar.equals(restDynamicVar)) {
                    newMMap.put(dynamicVar, textElement);
                    break;
                }
                DynamicVar<T> inter = dynamicVar.inter(restDynamicVar);

                if (inter.isEmpty()) continue;

                Set<StaticVar<T>> staticVars = newMMap.removeAll(dynamicVar);
                DynamicVar<T> diff = dynamicVar.minus(restDynamicVar);
                newMMap.putAll(inter, staticVars);
                if (!diff.isEmpty()) {
                    newMMap.putAll(diff, staticVars);
                }
                newMMap.put(inter, textElement);
                restDynamicVar = restDynamicVar.minus(dynamicVar);
            }

            if (!restDynamicVar.isEmpty()) {
                newMMap.put(restDynamicVar, textElement);
            }
            mmap = newMMap;
        }

//        _predicatesToVar = new HashMap<>();
        SetMultimap<Integer, StaticVar<T>> iToR = HashMultimap.create();
        int i = 0;
        for (DynamicVar<T> a : mmap.keySet()) {
//            _predicatesToVar.put(i, a);
            iToR.putAll(i, mmap.get(a));
            i++;
        }
        _numAtomicPredicates = i;
        _varToPredicates = Multimaps.asMap(Multimaps.invertFrom(iToR, HashMultimap.create()));
    }

    public int getNumAtomicPredicates() {
        return _numAtomicPredicates;
    }

    @Nonnull
    public Map<StaticVar<T>, Set<Integer>> getAtomicPredicates() {
        return _varToPredicates;
    }
}
