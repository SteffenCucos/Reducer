package com.scucos.maven.Reducer;

import com.scucos.maven.Reducer.Slice.SliceConstructionException;

public interface Reduceable<T> {
	Slice<T> toSlice() throws SliceConstructionException;
}
