/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.deployment.unit;

import org.jboss.as.deployment.DeploymentServiceListener;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Values;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test to verify the DeploymentChainProcessorInjector can correctly add a DeploymentUnitProcessor to a DeploymentChain 
 * through service injection.
 *
 * @author John E. Bailey
 */
public class DeploymentChainProcessorInjectorTestCase {


    @Test
    public void testInject() throws Exception {
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
        final BatchBuilder batchBuilder = serviceContainer.batchBuilder();
        final DeploymentServiceListener listener = new DeploymentServiceListener();
        batchBuilder.addListener(listener);

        final ServiceName chainServiceName = ServiceName.of("deployment", "chain");
        batchBuilder.addService(chainServiceName, new DeploymentChainService(Values.immediateValue((DeploymentChain)new DeploymentChainImpl("test.chain"))));

        final ServiceName processorServiceName = ServiceName.of("deployment", "processor");
        final DeploymentUnitProcessorService deploymentUnitProcessorService = new DeploymentUnitProcessorService(Values.immediateValue(new MockProcessor()));
        batchBuilder.addService(processorServiceName, deploymentUnitProcessorService)
            .addDependency(chainServiceName).toInjector(new DeploymentChainProcessorInjector(deploymentUnitProcessorService, 100L));

        batchBuilder.install();
        listener.waitForCompletion();
        assertNotNull(serviceContainer.getService(processorServiceName));

        final ServiceController<?> serviceController = serviceContainer.getService(chainServiceName);
        assertNotNull(serviceController);

        final DeploymentChain deploymentChain = (DeploymentChain)serviceController.getValue();
        assertNotNull(deploymentChain);

        final Field processorsField = DeploymentChainImpl.class.getDeclaredField("orderedProcessors");
        processorsField.setAccessible(true);
        final Set<Object> processors = (Set<Object>)processorsField.get(deploymentChain);
        assertNotNull(processors);
        assertEquals(1, processors.size());
    }

    private static class MockProcessor implements DeploymentUnitProcessor {
        @Override
        public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        }
    }
}
