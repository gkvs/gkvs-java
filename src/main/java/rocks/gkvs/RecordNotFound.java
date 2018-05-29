/*
 *
 * Copyright 2018 gKVS authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rocks.gkvs;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import rocks.gkvs.protos.ValueResult;

public final class RecordNotFound implements Record {

	private final @Nullable Key requestKey;
	private final ValueResult result;
	
	public RecordNotFound(@Nullable Key requestKey, ValueResult result) {
		this.requestKey = requestKey;
		this.result = result;
	}

	@Override
	public long requestId() {
		return result.getRequestId();
	}

	@Override
	public boolean exists() {
		return false;
	}
	
	@Override
	public long version() {
		return -1L;
	}

	@Override
	public int ttl() {
		return -1;
	}

	@Override
	public NullableKey key() {
		return new NullableKey(requestKey);
	}

	@Override
	public NullableValue value() {
		return new NullableValue(null);
	}
	
	@Override
	public List<Value> valueList() {
		return Collections.emptyList();
	}

	@Override
	public Map<String, Value> valueMap() {
		return Collections.emptyMap();
	}

	@Override
	public String toString() {
		return "RECORD_NOT_FOUND [" + requestId() + "]";
	}
	
		
}
