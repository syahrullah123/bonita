/**
 * Copyright (C) 2015 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.core.process.instance.model.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class SProcessInstanceImplTest {

    @Test
    public void defaultInterruptingEventIdShouldBeMinusOne() {
        assertThat(new SProcessInstanceImpl().getInterruptingEventId()).isEqualTo(-1L);
    }

    @Test
    public void should_be_root_instance_when_callerId_is_less_than_zero() throws Exception {
        //given
        SProcessInstanceImpl instance = new SProcessInstanceImpl();
        instance.setCallerId(-1);

        //then
        assertThat(instance.isRootInstance()).isTrue();
    }

    @Test
    public void should_not_be_root_instance_when_callerId_is_greater_than_zero() throws Exception {
        //given
        SProcessInstanceImpl instance = new SProcessInstanceImpl();
        instance.setCallerId(1);

        //then
        assertThat(instance.isRootInstance()).isFalse();
    }
}
