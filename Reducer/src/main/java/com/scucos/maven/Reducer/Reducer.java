package com.scucos.maven.Reducer;

import java.util.Set;
import java.util.Collection;
import java.util.Map;

public interface Reducer<T extends Mergeable<T>> {
	Set<T> reduce(Set<T> ts);
	
	T fromMap(Map<Object, Collection<Object>> slice);
}
