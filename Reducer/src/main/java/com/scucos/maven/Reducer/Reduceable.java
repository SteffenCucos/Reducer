package com.scucos.maven.Reducer;

public interface Reduceable<T> {
	Slice<T> toSlice();
}
