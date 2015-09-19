/*
Copyright 2015 Prosoft, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.prosoftnearshore.scope;

import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class WrapperScopeTest {

	@Test
	public void resourcesAddedLastAreClosedFirst() throws Exception {
		AutoCloseable closeable1 = mock(AutoCloseable.class, "closeable1");
		AutoCloseable closeable2 = mock(AutoCloseable.class, "closeable2");
		//create inOrder object passing any mocks that need to be verified in order
		InOrder inOrder = inOrder(closeable1, closeable2);

		WrapperScope w = WrapperScope.getNew();
		w.add(closeable1);
		w.add(closeable2);
		w.close();

		//following will make sure that closeable2 was closed before closeable1
		inOrder.verify(closeable2).close();
		inOrder.verify(closeable1).close();
	}
}