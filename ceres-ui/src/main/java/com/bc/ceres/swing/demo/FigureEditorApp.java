package com.bc.ceres.swing.demo;

import com.bc.ceres.glayer.swing.AdjustableViewScrollPane;
import com.bc.ceres.swing.actions.CopyAction;
import com.bc.ceres.swing.actions.CutAction;
import com.bc.ceres.swing.actions.DeleteAction;
import com.bc.ceres.swing.actions.PasteAction;
import com.bc.ceres.swing.actions.RedoAction;
import com.bc.ceres.swing.actions.SelectAllAction;
import com.bc.ceres.swing.actions.UndoAction;
import com.bc.ceres.swing.figure.AbstractInteractorListener;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.Interactor;
import com.bc.ceres.swing.figure.interactions.InsertEllipseFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertLineFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertPolygonFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertPolylineFigureInteractor;
import com.bc.ceres.swing.figure.interactions.InsertRectangleFigureInteractor;
import com.bc.ceres.swing.figure.interactions.PanInteractor;
import com.bc.ceres.swing.figure.interactions.SelectionInteractor;
import com.bc.ceres.swing.figure.interactions.ZoomInteractor;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.FigureEditorPanel;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import com.bc.ceres.swing.selection.support.DefaultSelectionManager;
import com.bc.ceres.swing.undo.support.DefaultUndoContext;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.prefs.Preferences;

public abstract class FigureEditorApp {
    private static final Interactor SELECTION_INTERACTOR = new SelectionInteractor();
    private static final Interactor ZOOM_INTERACTOR = new ZoomInteractor();
    private static final Interactor PAN_INTERACTOR = new PanInteractor();
    private static final InsertFigureInteractor NEW_LINE_INTERACTOR = new InsertLineFigureInteractor();
    private static final InsertFigureInteractor NEW_RECT_INTERACTOR = new InsertRectangleFigureInteractor();
    private static final InsertFigureInteractor NEW_ELLI_INTERACTOR = new InsertEllipseFigureInteractor();
    private static final InsertFigureInteractor NEW_POLYLINE_INTERACTOR = new InsertPolylineFigureInteractor();
    private static final InsertFigureInteractor NEW_POLYGON_INTERACTOR = new InsertPolygonFigureInteractor();

    private JFrame frame;

    private UndoAction undoAction;
    private RedoAction redoAction;
    private DeleteAction deleteAction;
    private SelectAllAction selectAllAction;
    private CutAction cutAction;
    private CopyAction copyAction;
    private PasteAction pasteAction;
    private FigureEditorPanel figureEditorPanel;

    static {
        Locale.setDefault(Locale.ENGLISH);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // ok
        }
    }

    protected FigureEditorApp() {
    }

    private void init() {

        DefaultSelectionManager selectionManager = new DefaultSelectionManager();
        DefaultUndoContext undoContext = new DefaultUndoContext(this);

        figureEditorPanel = new FigureEditorPanel(undoContext);
        selectionManager.setSelectionContext(figureEditorPanel.getFigureEditor().getSelectionContext());

        undoAction = new UndoAction(undoContext) {
            @Override
            public void execute() {
                super.execute();
                redoAction.updateState();
            }
        };
        redoAction = new RedoAction(undoContext) {
            @Override
            public void execute() {
                super.execute();
                undoAction.updateState();
            }
        };
        cutAction = new CutAction(selectionManager);
        copyAction = new CopyAction(selectionManager);
        pasteAction = new PasteAction(selectionManager);
        selectAllAction = new SelectAllAction(selectionManager);
        deleteAction = new DeleteAction(selectionManager);

        FigureFactory figureFactory = getFigureFactory();
        // todo - set FigureFactory on FigureEditor?
        NEW_LINE_INTERACTOR.setFigureFactory(figureFactory);
        NEW_RECT_INTERACTOR.setFigureFactory(figureFactory);
        NEW_ELLI_INTERACTOR.setFigureFactory(figureFactory);
        NEW_POLYLINE_INTERACTOR.setFigureFactory(figureFactory);
        NEW_POLYGON_INTERACTOR.setFigureFactory(figureFactory);

        AbstractButton selectButton = createInteractorButton(figureEditorPanel, "S", SELECTION_INTERACTOR);
        AbstractButton zoomButton = createInteractorButton(figureEditorPanel, "Z", ZOOM_INTERACTOR);
        AbstractButton panButton = createInteractorButton(figureEditorPanel, "P", PAN_INTERACTOR);
        AbstractButton newLineButton = createInteractorButton(figureEditorPanel, "L", NEW_LINE_INTERACTOR);
        AbstractButton newRectButton = createInteractorButton(figureEditorPanel, "R", NEW_RECT_INTERACTOR);
        AbstractButton newElliButton = createInteractorButton(figureEditorPanel, "E", NEW_ELLI_INTERACTOR);
        AbstractButton newPLButton = createInteractorButton(figureEditorPanel, "PL", NEW_POLYLINE_INTERACTOR);
        AbstractButton newPGButton = createInteractorButton(figureEditorPanel, "PG", NEW_POLYGON_INTERACTOR);

        JToolBar toolBar = new JToolBar();
        toolBar.add(selectButton);
        toolBar.add(zoomButton);
        toolBar.add(panButton);
        toolBar.add(newLineButton);
        toolBar.add(newRectButton);
        toolBar.add(newElliButton);
        toolBar.add(newPLButton);
        toolBar.add(newPGButton);

        ButtonGroup group = new ButtonGroup();
        group.add(selectButton);
        group.add(zoomButton);
        group.add(panButton);
        group.add(newLineButton);
        group.add(newRectButton);
        group.add(newElliButton);
        group.add(newPLButton);
        group.add(newPGButton);

        figureEditorPanel.getFigureEditor().setInteractor(SELECTION_INTERACTOR);
        figureEditorPanel.setPreferredSize(new Dimension(1024, 1024));

        FigureCollection drawing = figureEditorPanel.getFigureEditor().getFigureCollection();

        drawing.addFigure(figureFactory.createPolygonalFigure(new Rectangle(20, 30, 200, 100), DefaultFigureStyle.createShapeStyle(Color.BLUE, Color.GREEN)));
        drawing.addFigure(figureFactory.createPolygonalFigure(new Rectangle(90, 10, 100, 200), DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.ORANGE)));
        Path2D linePath = rectPath(true, 110, 60, 70, 140);
        drawing.addFigure(figureFactory.createLinealFigure(linePath, DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.BLACK)));

        linePath = new Path2D.Double();
        linePath.moveTo(110, 60);
        linePath.lineTo(110 + 70, 60);
        linePath.lineTo(110 + 70, 60 + 140);
        drawing.addFigure(figureFactory.createLinealFigure(linePath, DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.BLACK)));

        linePath = new Path2D.Double();
        linePath.moveTo(200, 100);
        linePath.lineTo(300, 200);
        drawing.addFigure(figureFactory.createLinealFigure(linePath, DefaultFigureStyle.createShapeStyle(Color.MAGENTA, Color.BLACK, new BasicStroke(10))));

        drawing.addFigure(figureFactory.createPolygonalFigure(new Ellipse2D.Double(50, 100, 80, 80), DefaultFigureStyle.createShapeStyle(Color.YELLOW, Color.RED)));
        drawing.addFigure(figureFactory.createPolygonalFigure(new Ellipse2D.Double(220, 120, 150, 300), DefaultFigureStyle.createShapeStyle(Color.GREEN, Color.BLUE)));

        Area area = new Area(new Rectangle(0, 0, 100, 100));
        area.subtract(new Area(new Rectangle(25, 25, 50, 50)));
        area.add(new Area(new Rectangle(75, 75, 50, 50)));
        area.subtract(new Area(new Rectangle(87, 87, 25, 25)));
        area.subtract(new Area(new Rectangle(-26, -26, 50, 50)));
//        drawing.addFigure(figureFactory.createPolygonalFigure(area, DefaultFigureStyle.createShapeStyle(Color.RED, Color.ORANGE)));

        Path2D path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        path.append(rectPath(true, 12, 12, 25, 25), false);
        path.append(rectPath(false, 65, 65, 25, 25), false);
        path.append(rectPath(false, 0, 0, 100, 100), false);
        DefaultFigureStyle shapeStyle = DefaultFigureStyle.createShapeStyle(new Color(0, 0, 255, 127), Color.ORANGE);
        drawing.addFigure(figureFactory.createPolygonalFigure(path, shapeStyle));

        path = new Path2D.Double(Path2D.WIND_NON_ZERO);
        path.append(rectPath(true, 12, 12, 25, 25), false);
        path.append(rectPath(false, 65, 65, 25, 25), false);
        path.append(rectPath(true, 0, 0, 100, 100), false);
//       drawing.addFigure(figureFactory.createPolygonalFigure(path, DefaultFigureStyle.createShapeStyle(new Color(255, 70, 128, 127), Color.ORANGE)));
        /*
        Area a2 = new Area();
        a2.add(new Area(new Rectangle(0, 0, 100, 100)));
        a2.subtract(new Area(new Rectangle(12, 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(65, 65, 25, 25)));
        a2.add(new Area(new Rectangle(200, 200, 100, 100)));
        a2.subtract(new Area(new Rectangle(200 + 12, 200 + 12, 25, 25)));
        a2.subtract(new Area(new Rectangle(200 + 65, 200 + 65, 25, 25)));
        drawing.addFigure(figureFactory.createPolygonalFigure(a2, DefaultFigureStyle.createShapeStyle(new Color(255, 255, 0, 127), Color.ORANGE)));
        */

        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createFileMenu());
        menuBar.add(createEditMenu());

        JComponent contentPane = new AdjustableViewScrollPane(figureEditorPanel);

        frame = new JFrame(getClass().getSimpleName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setJMenuBar(menuBar);
        frame.getContentPane().add(toolBar, BorderLayout.NORTH);
        frame.getContentPane().add(contentPane, BorderLayout.CENTER);
        frame.setSize(400, 400);

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                e.printStackTrace();
                String message = MessageFormat.format("" +
                        "An internal error occured!\n" +
                        "Type: {0}\n" +
                        "Message: {1}", e.getClass(), e.getMessage());
                JOptionPane.showMessageDialog(frame, message,
                                              "Internal Error",
                                              JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });

        figureEditorPanel.getFigureEditor().getSelectionContext().addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                System.out.println("selection changed: " + event.getSelection());
            }

            @Override
            public void selectionContextChanged(SelectionChangeEvent event) {
                System.out.println("selection context changed: " + event.getSelection());
            }
        });

        undoContext.addUndoableEditListener(new UndoableEditListener() {
            @Override
            public void undoableEditHappened(UndoableEditEvent event) {
                System.out.println("edit happened: " + event.getEdit());
            }
        });

        selectionManager.getClipboard().addFlavorListener(new FlavorListener() {
            @Override
            public void flavorsChanged(FlavorEvent event) {
                System.out.println("flavors changed: " + event);
            }
        });
    }


    private static Path2D rectPath(boolean clockwise, int x, int y, int w, int h) {
        Path2D.Double linePath = new Path2D.Double();
        linePath.moveTo(x, y);
        if (clockwise) {
            linePath.lineTo(x, y + h);
            linePath.lineTo(x + w, y + h);
            linePath.lineTo(x + w, y);
        } else {
            linePath.lineTo(x + w, y);
            linePath.lineTo(x + w, y + h);
            linePath.lineTo(x, y + h);
        }
        linePath.lineTo(x, y);
        linePath.closePath();
        return linePath;
    }

    public static void main(String[] args) {
        run(new FigureEditorApp() {
            @Override
            protected FigureFactory getFigureFactory() {
                return new DefaultFigureFactory();
            }

            @Override
            protected void loadFigureCollection(File file, FigureCollection figureCollection) {
                JOptionPane.showMessageDialog(getFrame(), "Not implemented.");
            }

            @Override
            protected void storeFigureCollection(FigureCollection figureCollection, File file) {
                JOptionPane.showMessageDialog(getFrame(), "Not implemented.");
            }
        });
    }

    public static void run(FigureEditorApp drawingApp) {
        drawingApp.init();
        drawingApp.run();
    }

    protected abstract FigureFactory getFigureFactory();

    protected abstract void loadFigureCollection(File file, FigureCollection figureCollection);

    protected abstract void storeFigureCollection(FigureCollection figureCollection, File file);

    public JFrame getFrame() {
        return frame;
    }

    private void run() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frame.setVisible(true);
            }
        });
    }

    private JMenu createFileMenu() {
        JMenu menu = new JMenu("File");
        menu.add(new OpenAction());
        menu.add(new SaveAsAction());
        menu.add(new ExitAction());
        return menu;
    }

    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.add(undoAction);
        menu.add(redoAction);
        menu.addSeparator();
        menu.add(cutAction);
        menu.add(copyAction);
        menu.add(pasteAction);
        menu.addSeparator();
        menu.add(selectAllAction);
        menu.addSeparator();
        menu.add(deleteAction);
        return menu;
    }

    private static AbstractButton createInteractorButton(final FigureEditorPanel figureEditorPanel, String name, final Interactor interactor) {
        final AbstractButton selectButton = new JToggleButton(name);
        selectButton.setSelected(false);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                figureEditorPanel.getFigureEditor().setInteractor(interactor);
            }
        });
        interactor.addListener(new AbstractInteractorListener() {
            @Override
            public void interactorActivated(Interactor interactor) {
                selectButton.setSelected(true);
            }

            @Override
            public void interactorDeactivated(Interactor interactor) {
                selectButton.setSelected(false);
            }
        });
        return selectButton;
    }

    private class OpenAction extends AbstractAction {
        private OpenAction() {
            putValue(Action.NAME, "Open...");
            putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File lastDir = new File(Preferences.userNodeForPackage(FigureEditorApp.class).get("lastDir", "."));
            JFileChooser chooser = new JFileChooser(lastDir);
            chooser.setAcceptAllFileFilterUsed(true);
            int i = chooser.showOpenDialog(frame);
            if (i == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                Preferences.userNodeForPackage(FigureEditorApp.class).put("lastDir", chooser.getCurrentDirectory().getPath());
                figureEditorPanel.getFigureEditor().getFigureSelection().removeFigures();
                figureEditorPanel.getFigureEditor().getFigureCollection().removeFigures();
                loadFigureCollection(chooser.getSelectedFile(), figureEditorPanel.getFigureEditor().getFigureCollection());
            }
        }
    }

    private class SaveAsAction extends AbstractAction {
        private SaveAsAction() {
            putValue(Action.NAME, "Save As...");
            putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File lastDir = new File(Preferences.userNodeForPackage(FigureEditorApp.class).get("lastDir", "."));
            JFileChooser chooser = new JFileChooser(lastDir);
            chooser.setAcceptAllFileFilterUsed(true);
            int i = chooser.showSaveDialog(frame);
            if (i == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
                Preferences.userNodeForPackage(FigureEditorApp.class).put("lastDir", chooser.getCurrentDirectory().getPath());
                storeFigureCollection(figureEditorPanel.getFigureEditor().getFigureCollection(), chooser.getSelectedFile());
            }
        }
    }

    private class ExitAction extends AbstractAction {
        private ExitAction() {
            putValue(Action.NAME, "Exit");
            putValue(Action.ACTION_COMMAND_KEY, getClass().getName());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            frame.dispose();
        }
    }

}
