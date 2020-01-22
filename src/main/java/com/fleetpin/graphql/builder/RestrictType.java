package com.fleetpin.graphql.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface RestrictType<T> {

	public CompletableFuture<Boolean> allow(T obj);
	
	public default CompletableFuture<List<T>> filter(List<T> list) {
		
		boolean[] keep = new boolean[list.size()];
		
		CompletableFuture<?>[] all = new CompletableFuture[list.size()];
		for(int i = 0; i < list.size(); i++) {
			int offset = i;
			all[i] = allow(list.get(i)).thenAccept(allow -> keep[offset] = allow);
		}
		return CompletableFuture.allOf(all).thenApply(__ -> {
			List<T> toReturn = new ArrayList<>();
			for(int i = 0; i < list.size(); i++) {
				if(keep[i]) {
					toReturn.add(list.get(i));
				}
			}
			return toReturn;
		});
	}
	
	
}
