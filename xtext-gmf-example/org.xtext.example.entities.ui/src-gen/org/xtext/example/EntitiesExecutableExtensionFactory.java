
/*
 * generated by Xtext
 */
 
package org.xtext.example;

import org.eclipse.xtext.ui.core.guice.AbstractGuiceAwareExecutableExtensionFactory;
import org.osgi.framework.Bundle;

import com.google.inject.Injector;

/**
 *@generated
 */
public class EntitiesExecutableExtensionFactory extends AbstractGuiceAwareExecutableExtensionFactory {

	@Override
	protected Bundle getBundle() {
		return org.xtext.example.internal.EntitiesActivator.getInstance().getBundle();
	}
	
	@Override
	protected Injector getInjector() {
		return org.xtext.example.internal.EntitiesActivator.getInstance().getInjector("org.xtext.example.Entities");
	}
	
}
