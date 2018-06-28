/*
 *
 * Copyright 2018-present GKVS authors.
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

import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import rocks.gkvs.value.Str;

/**
 * 
 * PutRemoveFutureTest
 *
 * @author Alex Shvid
 * @date Jun 18, 2018 
 *
 */

public class PutRemoveFutureTest extends AbstractClientTest {

	private Key KEY = Key.raw(STORE, UUID.randomUUID().toString());
	
	@Test
	public void testPutFuture() {
		
		Assert.assertFalse(Gkvs.Client.get(KEY).metadataOnly().sync().exists());
		
		GkvsFuture<Status> future = Gkvs.Client.put(KEY, new Str("val")).async();
		
		Assert.assertTrue(future.getUnchecked().updated());
		
		Assert.assertTrue(Gkvs.Client.get(KEY).metadataOnly().sync().exists());
		
		future = Gkvs.Client.remove(KEY).async();
		
		Assert.assertTrue(future.getUnchecked().updated());
		
		Assert.assertFalse(Gkvs.Client.get(KEY).metadataOnly().sync().exists());
		
	}
}
