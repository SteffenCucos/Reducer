package com.scucos.maven.Reducer;

import java.util.Set;
import java.util.Collection;
import java.util.Map;

public interface Reducer<T extends Reduceable<T>> {
	Set<T> reduce(Set<T> ts);
	
	T fromSlice(Slice<T> slice);
}
