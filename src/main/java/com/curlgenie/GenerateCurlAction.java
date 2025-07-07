package com.curlgenie;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.Messages;

public class GenerateCurlAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        var project = e.getProject();
        var editor = e.getData(com.intellij.openapi.actionSystem.CommonDataKeys.EDITOR);

        if (editor == null || project == null) {
            Messages.showErrorDialog("No editor or project context available.", "Error");
            return;
        }

        var selectionModel = editor.getSelectionModel();
        String methodInput = selectionModel.getSelectedText();

        if (methodInput == null || methodInput.trim().isEmpty()) {
            Messages.showInfoMessage(project, "Please select a Spring method in the editor before running the action.", "No Method Selected");
            return;
        }

        String classInput = Messages.showMultilineInputDialog(
                e.getProject(),
                "Paste related class definitions (optional):", // message
                "Class Definitions",                           // title
                "",                                            // initial value
                Messages.getQuestionIcon(),                    // icon
                null                                           // validator
        );

        if (classInput == null) classInput = "";

        CurlGenerator generator = new CurlGenerator();
        String curl = generator.generateCurl(methodInput, classInput);

        // ✅ Create a virtual file and open in editor
        com.intellij.openapi.vfs.VirtualFile file = createVirtualCurlFile(project, curl);
        if (file != null) {
            com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(file, true);
        }
    }

    private com.intellij.openapi.vfs.VirtualFile createVirtualCurlFile(com.intellij.openapi.project.Project project, String curlContent) {
        String fileName = "Generated-curl-" + System.currentTimeMillis() + ".sh";
        com.intellij.testFramework.LightVirtualFile virtualFile = new com.intellij.testFramework.LightVirtualFile(
                fileName,
                com.intellij.openapi.fileTypes.PlainTextFileType.INSTANCE,
                curlContent
        );
        virtualFile.setWritable(false);

        return virtualFile;
    }

}