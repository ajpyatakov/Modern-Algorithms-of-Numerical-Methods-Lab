package com.amm.manmlab.controller;

import com.amm.manmlab.algorithms.Algorithm;
import com.amm.manmlab.errors.ErrorMessages;
import com.amm.manmlab.ui.DialogPanel;
import com.amm.manmlab.utils.fileinput.FileInputLoader;
import com.amm.manmlab.ui.MainFrame;
import com.amm.manmlab.utils.containers.FiniteElementMethodInput;
import com.amm.manmlab.utils.containers.PointsWithAdjacencyMatrix;
import com.amm.manmlab.utils.containers.PointsWithEdges;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);
    
    private FileInputLoader fileInputLoader;
    private Algorithm<PointsWithEdges, PointsWithEdges> triangulationAlgorithm;
    private Algorithm<PointsWithAdjacencyMatrix, PointsWithAdjacencyMatrix> renumberingAlgorithm;
    private Algorithm<FiniteElementMethodInput, Double[]> finiteElementMethodAlgorithm;
    private Algorithm<PointsWithEdges, Boolean> polygonGridValidator;
    private Algorithm<PointsWithEdges, Boolean> triangulationGridValidator;
    private MainFrame mainFrame;
    
    private PointsWithEdges initialPointsWithEdges;
    private PointsWithEdges pointsWithEdgesForSettingEdge;
    private PointsWithEdges pointsWithEdgesAfterTriangulation;

    public MainController(FileInputLoader fileInputLoader,
            Algorithm<PointsWithEdges, PointsWithEdges> triangulationAlgorithm,
            Algorithm<PointsWithAdjacencyMatrix, PointsWithAdjacencyMatrix> renumberingAlgorithm,
            Algorithm<FiniteElementMethodInput, Double[]> finiteElementMethodAlgorithm,
            Algorithm<PointsWithEdges, Boolean> polygonGridValidator,
            Algorithm<PointsWithEdges, Boolean> triangulationGridValidator) {
        this.fileInputLoader = fileInputLoader;
        this.triangulationAlgorithm = triangulationAlgorithm;
        this.renumberingAlgorithm = renumberingAlgorithm;
        this.finiteElementMethodAlgorithm = finiteElementMethodAlgorithm;
        this.polygonGridValidator = polygonGridValidator;
        this.triangulationAlgorithm = triangulationAlgorithm;
    }
    
    public void startApplication() {
        loadData();
        loadUI();
        initBasicListeners();
        initListenersForSettingEdgeDialog();
        mainFrame.setVisible(true);
    }

    private void loadData() {
        initialPointsWithEdges = fileInputLoader.loadInputFromFile();
    }
    
    private void loadUI() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException | UnsupportedLookAndFeelException exception) {
            exception.printStackTrace();
        }
        mainFrame = new MainFrame();
    }
    
    private void initBasicListeners() {
        mainFrame.getStepBackButton().addActionListener(event -> makeStepBack());
        DialogPanel dialogPanel = mainFrame.getDialogPanel();
        dialogPanel.getSettingEdgeDialog().getCleanButton().addActionListener(
                event -> initListenersForSettingEdgeDialog(new PointsWithEdges(new ArrayList<>(), new ArrayList<>())));
        dialogPanel.getSettingEdgeDialog().getReadFileButton().addActionListener(
                event -> initListenersForSettingEdgeDialog());
        dialogPanel.getSettingEdgeDialog().getManualTriangulationButton().addActionListener(
                event -> initListenersForManualTriangulationDialog());
        dialogPanel.getSettingEdgeDialog().getAutoTriangulationButton().addActionListener(
                event -> makeAutoTriangulation());
    }
    
    private void initListenersForSettingEdgeDialog() {
        initListenersForSettingEdgeDialog(initialPointsWithEdges.clone());
    }
    
    private void initListenersForSettingEdgeDialog(PointsWithEdges pointsWithEdges) {
        mainFrame.getDialogPanel().setCurrentDialog(DialogPanel.SETTING_EDGE_DIALOG);
        pointsWithEdgesForSettingEdge = pointsWithEdges;
        SimplePaintStrategy paintStrategy = new SimplePaintStrategy(pointsWithEdges);
        mainFrame.getImagePanel().removeAllImagePanelListeners();
        mainFrame.getImagePanel().addImagePanelListener(new SettingEdgeController(paintStrategy));
        mainFrame.getImagePanel().setImagePanelPaintStrategy(paintStrategy);
        mainFrame.getImagePanel().repaint();
    }
    
    private void initListenersForManualTriangulationDialog() {
        mainFrame.getDialogPanel().setCurrentDialog(DialogPanel.MANUAL_TRIANGULATION_DIALOG);
    }
    
    private void initListenersForTriangulationResultDialog() {
        mainFrame.getDialogPanel().setCurrentDialog(DialogPanel.TRIANGULATION_RESULT_DIALOG);
        mainFrame.getImagePanel().removeAllImagePanelListeners();
        mainFrame.getImagePanel().setImagePanelPaintStrategy(new SimplePaintStrategy(pointsWithEdgesAfterTriangulation));
        mainFrame.getImagePanel().repaint();
    }
    
    private void initListenersForSettingEdgeConditionsDialog() {
        mainFrame.getDialogPanel().setCurrentDialog(DialogPanel.SETTING_EDGE_CONDITIONS_DIALOG);
    }
    
    private void initListenersForResultDialog() {
        mainFrame.getDialogPanel().setCurrentDialog(DialogPanel.RESULT_DIALOG);
    }
    
    private void makeStepBack() {
        switch (mainFrame.getDialogPanel().getCurrentDialogIdentifier()) {
            case DialogPanel.SETTING_EDGE_DIALOG:
                break;
            case DialogPanel.MANUAL_TRIANGULATION_DIALOG:
                initListenersForSettingEdgeDialog(pointsWithEdgesForSettingEdge);
                break;
            case DialogPanel.TRIANGULATION_RESULT_DIALOG:
                initListenersForSettingEdgeDialog(pointsWithEdgesForSettingEdge);
                break;
            case DialogPanel.SETTING_EDGE_CONDITIONS_DIALOG:
                initListenersForTriangulationResultDialog();
                break;
            case DialogPanel.RESULT_DIALOG:
                initListenersForSettingEdgeConditionsDialog();
                break;
        }
    }
    
    private void makeAutoTriangulation() {
        try {
            boolean gridIsValid = polygonGridValidator.doAlgorithm(pointsWithEdgesForSettingEdge);
            if (!gridIsValid) {
                throw new IllegalStateException(ErrorMessages.GRID_MUST_BE_POLYGON.getMessage());
            }
            pointsWithEdgesAfterTriangulation = triangulationAlgorithm.doAlgorithm(pointsWithEdgesForSettingEdge);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
            showMessage(ex.getMessage());
            return;
        }
        initListenersForTriangulationResultDialog();
    }
    
    private void showMessage(String message) {
        JOptionPane.showMessageDialog(mainFrame, message, "Exception", JOptionPane.INFORMATION_MESSAGE);
    }

}