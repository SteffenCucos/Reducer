package com.scucos.maven.Reducer;
import java.util.Collection;
import java.util.Map;

public interface Mergeable<T> {
	Map<Object,Collection<Object>> toMap();
}
