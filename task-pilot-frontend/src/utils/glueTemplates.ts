export const GLUE_SOURCE_TEMPLATES: Record<string, string> = {
  GLUE_GROOVY: `package com.ruishanio.taskpilot.service.handler;

import com.ruishanio.taskpilot.core.context.TaskPilotHelper;
import com.ruishanio.taskpilot.core.handler.ITaskHandler;

public class DemoGlueTaskHandler extends ITaskHandler {
    @Override
    public void execute() throws Exception {
        TaskPilotHelper.log("TASK-PILOT, Hello World.");
    }
}`,
  GLUE_SHELL: `#!/bin/bash
echo "task-pilot: hello shell"`,
  GLUE_PYTHON: `print("task-pilot: hello python3")`,
  GLUE_PYTHON2: `print "task-pilot: hello python2"`,
  GLUE_NODEJS: `console.log("task-pilot: hello nodejs");`,
  GLUE_POWERSHELL: `Write-Host "task-pilot: hello powershell"`,
  GLUE_PHP: `<?php echo "task-pilot: hello php";`,
};
