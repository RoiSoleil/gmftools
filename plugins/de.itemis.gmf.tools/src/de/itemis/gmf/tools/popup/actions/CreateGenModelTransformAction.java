/*******************************************************************************
 * Copyright (c) 2008 itemis AG (http://www.itemis.eu) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package de.itemis.gmf.tools.popup.actions;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;

import de.itemis.gmf.tools.contribution.GmfGenModelTransformer;
import de.itemis.gmf.tools.preferences.GmfModel;

public class CreateGenModelTransformAction extends GMFToolsAction {

	public CreateGenModelTransformAction() {
		super("gmfgen");
	}

	public void run(IAction action) {
		for(GmfModel gmfModel : gmfModels) {
			GmfGenModelTransformer.createOrGetTransformationFile(gmfModel, new NullProgressMonitor());			
		}
	}

}
