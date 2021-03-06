/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.xd.dirt.store;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.util.Assert;

/**
 * Base implementation for an in-memory store, using a {@link Map} internally.
 * 
 * Default behaviour is to retain sort order on the keys. Hence, this is by default the only sort supported when
 * querying with a {@link Pageable}.
 * 
 * @param <T> the type of things to store
 * @param <ID> a "primary key" to the things
 * @author Eric Bottard
 */
public abstract class AbstractInMemoryRepository<T, ID extends Serializable> implements
		PagingAndSortingRepository<T, ID> {

	private final Map<ID, T> map;

	protected AbstractInMemoryRepository() {
		map = buildMap();
	}

	protected Map<ID, T> buildMap() {
		Map<ID, T> map = new TreeMap<ID, T>();
		return Collections.synchronizedMap(map);
	}

	@Override
	public <S extends T> S save(S entity) {
		Assert.notNull(entity);
		map.put(keyFor(entity), entity);
		return entity;
	}

	protected abstract ID keyFor(T entity);

	@Override
	public <S extends T> Iterable<S> save(Iterable<S> entities) {
		List<S> result = new ArrayList<S>();
		for (S entity : entities) {
			result.add(save(entity));
		}
		return result;
	}

	@Override
	public T findOne(ID id) {
		return map.get(id);
	}

	@Override
	public boolean exists(ID id) {
		return map.containsKey(id);
	}

	@Override
	public Iterable<T> findAll() {
		return new ArrayList<T>(map.values());
	}

	@Override
	public Iterable<T> findAll(Iterable<ID> ids) {
		List<T> result = new ArrayList<T>();
		for (ID id : ids) {
			T one = findOne(id);
			if (one != null) {
				result.add(one);
			}
		}
		return result;
	}

	@Override
	public long count() {
		return map.size();
	}

	@Override
	public void delete(ID id) {
		map.remove(id);
	}

	@Override
	public void delete(T entity) {
		map.remove(keyFor(entity));
	}

	@Override
	public void delete(Iterable<? extends T> entities) {
		for (T entity : entities) {
			delete(entity);
		}
	}

	@Override
	public void deleteAll() {
		map.clear();
	}

	@Override
	public Page<T> findAll(Pageable pageable) {
		Assert.isNull(pageable.getSort(), "Arbitrary sorting is not implemented");
		return slice((List<T>) findAll(), pageable);
	}

	/**
	 * Post-process the list to only return elements matching the page request.
	 */
	protected Page<T> slice(List<T> list, Pageable pageable) {
		int to = Math.min(list.size(), pageable.getOffset() + pageable.getPageSize());
		List<T> data = list.subList(pageable.getOffset(), to);
		return new PageImpl<T>(data, pageable, list.size());
	}

	@Override
	public Iterable<T> findAll(Sort sort) {
		throw new UnsupportedOperationException("Arbitrary sorting is not implemented");
	}

}
