package org.xtext.example.entities.diagram.edit.part;

import org.eclipse.gmf.runtime.notation.View;
import org.eclipse.xtext.gmf.glue.editingdomain.SemanticRootUnloadListener;
import org.xtext.example.entities.diagram.edit.parts.ModelEditPart;

public class ModelEditPartOverride extends ModelEditPart {

	private SemanticRootUnloadListener semanticRootUnloadListener;

	public ModelEditPartOverride(View view) {
		super(view);
		semanticRootUnloadListener = new SemanticRootUnloadListener(this);
	}

	@Override
	public void activate() {
		super.activate();
		semanticRootUnloadListener.activate();
	}
	
	@Override
	public void deactivate() {
		super.deactivate();
		semanticRootUnloadListener.deactivate();
	}
}
