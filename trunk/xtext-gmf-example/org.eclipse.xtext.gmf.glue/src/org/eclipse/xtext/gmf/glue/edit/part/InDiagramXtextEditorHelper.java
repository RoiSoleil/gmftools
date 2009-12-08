package org.eclipse.xtext.gmf.glue.edit.part;

import java.util.Arrays;
import java.util.LinkedList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.draw2d.Figure;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.gef.EditPartViewer;
import org.eclipse.gmf.runtime.diagram.ui.editparts.DiagramRootEditPart;
import org.eclipse.gmf.runtime.diagram.ui.editparts.IGraphicalEditPart;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditDomain;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.gmf.glue.Activator;
import org.eclipse.xtext.gmf.glue.editingdomain.UpdateXtextResourceTextCommand;
import org.eclipse.xtext.parsetree.CompositeNode;
import org.eclipse.xtext.parsetree.NodeAdapter;
import org.eclipse.xtext.parsetree.NodeUtil;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.core.editor.XtextEditor;
import org.eclipse.xtext.ui.core.editor.info.ResourceWorkingCopyFileEditorInput;
import org.eclipse.xtext.ui.core.editor.model.IXtextDocument;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com.google.inject.Injector;

public class InDiagramXtextEditorHelper {

	private static int MIN_EDITOR_WIDTH = 100;

	private static int MIN_EDITOR_HEIGHT = 20;

	private IGraphicalEditPart hostEditPart;

	private XtextEditor xtextEditor;

	private int editorOffset;

	private int initialEditorSize;

	private int initialDocumentSize;

	private Composite xtextEditorComposite;

	private IFigure artificialFigure;

	private EditPartViewer diagramViewer;

	private final Injector xtextInjector;

	private XtextResource xtextResource;

	private String semanticElementFragment;

	public InDiagramXtextEditorHelper(IGraphicalEditPart editPart,
			Injector xtextInjector) {
		this.hostEditPart = editPart;
		this.xtextInjector = xtextInjector;
	}

	public void showEditor() {
		try {
			EObject semanticElement = hostEditPart.resolveSemanticElement();
			if (semanticElement == null) {
				return;
			}
			Resource semanticResource = semanticElement.eResource();
			if (!(semanticResource instanceof XtextResource)) {
				return;
			}
			semanticElementFragment = semanticResource
					.getURIFragment(semanticElement);
			if (semanticElementFragment == null
					|| "".equals(semanticElementFragment)) {
				return;
			}
			xtextResource = (XtextResource) semanticResource;
			createXtextEditor(new ResourceWorkingCopyFileEditorInput(
					semanticResource));
			setEditorRegion();
			setEditorBounds();
		} catch (Exception e) {
			Activator.logError(e);
		} finally {
			if (hostEditPart != null) {
				hostEditPart.refresh();
			}
		}
	}

	public void closeEditor(boolean isReconcile) {
		if (xtextEditor != null) {
			final IFigure parent = artificialFigure.getParent();
			if (parent != null) {
				parent.remove(artificialFigure);
			}
			if (isReconcile) {
				try {
					final IXtextDocument xtextDocument = xtextEditor
							.getDocument();
					// subtract 2 for the artificial newlines
					int documentGrowth = xtextDocument.getLength()
							- initialDocumentSize - 2;
					String newText = xtextDocument.get(editorOffset + 1,
							initialEditorSize + documentGrowth);
					UpdateXtextResourceTextCommand.createICommand(
							xtextResource, editorOffset, initialEditorSize,
							newText).execute(null, null);
				} catch (Exception exc) {
					Activator.logError(exc);
				}
			}
			xtextEditor.dispose();
			xtextEditorComposite.setVisible(false);
			xtextEditorComposite.dispose();
			xtextEditor = null;
			// Hack: somehow the keybindings are lost when closing the xtext
			// editor.
			// They are restored on reactivation of the diagram editor.
			// TODO: find out why keybindings are lost and find better solution
//			final IEditorPart diagramEditorPart = getDiagramEditorPart(diagramViewer);
//			final IWorkbenchPage activePage = PlatformUI.getWorkbench()
//					.getActiveWorkbenchWindow().getActivePage();
//			activePage.activate(activePage.getViewReferences()[0]
//					.getView(false));
//			activePage.activate(diagramEditorPart);
		}
	}

	private void createXtextEditor(IEditorInput editorInput)
			throws CoreException {
		diagramViewer = hostEditPart.getRoot().getViewer();
		Composite diagramComposite = (Composite) diagramViewer.getControl();
		DiagramEditDomain diagramEditDomain = (DiagramEditDomain) diagramViewer
				.getEditDomain();
		IEditorSite diagramEditorSite = diagramEditDomain.getEditorPart()
				.getEditorSite();

		xtextEditor = xtextInjector.getInstance(XtextEditor.class);
		IEditorSite xtextEditorSite = diagramEditorSite;
		xtextEditor.init(xtextEditorSite, editorInput);
		createEditorPartControl(diagramComposite);
		addKeyListener();
	}

	private IEditorPart getDiagramEditorPart(EditPartViewer diagramViewer) {
		return ((DiagramEditDomain) diagramViewer.getEditDomain())
				.getEditorPart();
	}

	private void createEditorPartControl(Composite diagramComposite) {
		// TODO: find a more elegant way to get the xtextEditorComposite
		Control[] siblingsBefore = diagramComposite.getChildren();
		Composite diagramParentComposite = (Composite) diagramComposite;
		xtextEditor.createPartControl(diagramParentComposite);
		Control[] siblingsAfter = diagramComposite.getChildren();
		java.util.List<Control> controlSiblings = new LinkedList<Control>(
				Arrays.asList(siblingsAfter));
		controlSiblings.removeAll(Arrays.asList(siblingsBefore));
		xtextEditorComposite = (Composite) controlSiblings.get(0);
	}

	private void addKeyListener() {
		ISourceViewer sourceViewer = xtextEditor.getInternalSourceViewer();
		final StyledText xtextTextWidget = sourceViewer.getTextWidget();
		xtextTextWidget.addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent e) {
				if ((e.stateMask & SWT.CTRL) != 0
						&& ((e.keyCode == SWT.KEYPAD_CR) || (e.keyCode == SWT.CR))) {
					e.doit = false;
				}
			}
		});
		xtextTextWidget.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int keyCode = e.keyCode;
				if ((e.stateMask & SWT.CTRL) != 0
						&& ((keyCode == SWT.KEYPAD_CR) || (keyCode == SWT.CR))) {
					closeEditor(true);
				}
				if (keyCode == SWT.ESC) {
					closeEditor(false);
				}
			}
		});
	}

	private void setEditorRegion() throws BadLocationException {
		final IXtextDocument xtextDocument = xtextEditor.getDocument();
		boolean success = xtextEditor.getDocument().modify(
				new IUnitOfWork<Boolean, XtextResource>() {

					public Boolean exec(XtextResource state) throws Exception {
						EObject semanticElementInDocument = state
								.getEObject(semanticElementFragment);
						if (semanticElementInDocument == null) {
							return false;
						}
						CompositeNode xtextNode = getCompositeNode(semanticElementInDocument);
						if (xtextNode == null) {
							return false;
						}
						// getOffset() and getLength() are trimming whitespaces
						editorOffset = xtextNode.getOffset();
						initialEditorSize = xtextNode.getLength();
						initialDocumentSize = xtextDocument.getLength();

						// insert a newline directly before and after the node
						xtextDocument.replace(editorOffset, 0, "\n");
						xtextDocument.replace(editorOffset + 1
								+ initialEditorSize, 0, "\n");
						return true;
					}

				});

		if (success) {
			xtextEditor.showHighlightRangeOnly(true);
			xtextEditor.setHighlightRange(editorOffset + 1, initialEditorSize,
					true);
			xtextEditor.setFocus();
		}
	}

	private void setEditorBounds() {
		final IXtextDocument xtextDocument = xtextEditor.getDocument();
		// mind the added newlines
		String editString = "";
		try {
			editString = xtextDocument.get(editorOffset + 1, initialEditorSize);
		} catch (BadLocationException exc) {
			Activator.logError(exc);
		}
		int numLines = StringUtil.getNumLines(editString);
		int numColumns = StringUtil.getMaxColumns(editString);
		IFigure figure = hostEditPart.getFigure();
		Rectangle bounds = figure.getBounds().getCopy();
		DiagramRootEditPart diagramEditPart = (DiagramRootEditPart) hostEditPart
				.getRoot();
		IFigure contentPane = diagramEditPart.getContentPane();
		contentPane.translateToAbsolute(bounds);

		Font font = figure.getFont();
		FontData fontData = font.getFontData()[0];
		int fontHeightInPixel = fontData.getHeight();

		// TODO: this needs some work...
		int width = Math.max(fontHeightInPixel * (numColumns + 3),
				MIN_EDITOR_WIDTH);
		int height = Math.max(fontHeightInPixel * (numLines + 4),
				MIN_EDITOR_HEIGHT);
		xtextEditorComposite.setBounds(bounds.x, bounds.y, width, height);

		// add an artificial figure of same size to enable scrolling
		Rectangle boundsForArtificialFigure = new Rectangle(bounds.x, bounds.y,
				width, height);
		contentPane.translateToRelative(boundsForArtificialFigure);
		artificialFigure = new Figure();
		artificialFigure.setBounds(boundsForArtificialFigure);
		contentPane.add(artificialFigure);
	}

	private CompositeNode getCompositeNode(EObject semanticElement) {
		NodeAdapter nodeAdapter = NodeUtil.getNodeAdapter(semanticElement);
		if (nodeAdapter != null) {
			final CompositeNode parserNode = nodeAdapter.getParserNode();
			return parserNode;
		}
		return null;
	}

}
