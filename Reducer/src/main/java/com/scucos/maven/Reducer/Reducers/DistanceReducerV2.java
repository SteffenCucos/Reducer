package com.scucos.maven.Reducer.Reducers;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.scucos.maven.Reducer.Slice;

public abstract class DistanceReducerV2<T> implements Reducer<T> {

	@Override
	public Set<Slice<T>> reduceSlices(Set<Slice<T>> slices) {
		Set<Slice<T>> reduced = new HashSet<>();
		int startSize = slices.size();
		
		while(slices.size() > 0) {
			Slice<T> head = slices.stream().iterator().next();
			slices.remove(head);
			
			Set<Slice<T>> leftOver = new HashSet<>();
			for(Slice<T> slice : slices) {
				Tuple<Integer, Integer> diffTuple = difference(head, slice);
				int headIntoSlice = diffTuple.o1;
				int sliceIntoHead = diffTuple.o2;
				
				if(headIntoSlice == 0 && sliceIntoHead == 1) {
					//Slice is completely contained by head
					//We can ignore it
				} else if (headIntoSlice == 1 && sliceIntoHead == 0) {
					//Head is contained completely by slice
					//Replace head with slice?
					head = slice;
				} else if (headIntoSlice == 1 && sliceIntoHead == 1) {
					//They differ in one spot exactly
					head = mergeInto(slice, head);
				} else {
					leftOver.add(slice);
				}
			}
			reduced.add(head);
			slices = leftOver;
		}

		if(reduced.size() < startSize) {
			return reduceSlices(reduced);
		}
		
		return reduced;
	}
	
	public class Tuple<T1,T2> {
		T1 o1;
		T2 o2;
		
		public Tuple(T1 o1, T2 o2) {
			this.o1 = o1;
			this.o2 = o2;
		}
	}
	
	public Tuple<Integer, Integer> difference(Slice<T> s1, Slice<T> s2) {
		Set<String> categories = s1.getCategories();
		int S1IntoS2 = categories.size();
		int S2IntoS1 = categories.size();
		
		for(Object category : categories) {
			Collection<?> e1 = s1.getEntry(category);
			Collection<?> e2 = s2.getEntry(category);
			if(e1.containsAll(e2)) {
				S2IntoS1 -= 1;
			}
			if(e2.containsAll(e1)) {
				S1IntoS2 -= 1;
			}
		}
		
		return new Tuple<Integer, Integer>(S1IntoS2, S2IntoS1);
	}
	
	@SuppressWarnings("unchecked")
	public Slice<T> mergeInto(Slice<T> s1, Slice<T> s2) {
		Set<String> categories = s1.getCategories();
		for(Object category : categories) {
			Collection<Object> e1 = (Collection<Object>) s1.getEntry(category);
			Collection<Object> e2 = (Collection<Object>) s2.getEntry(category);
			e1.addAll(e2);
		}
		return s1;
	}
}