package org.openapi.codegen.cli.cmd;

import org.openapi.codegen.CliOption;
import org.openapi.codegen.CodegenConfig;
import org.openapi.codegen.CodegenConfigLoader;

public class ConfigHelp implements Runnable {
    private String lang;

    public void setLang(String lang) {
        this.lang = lang;
    }

    @Override
    public void run() {
        System.out.println();
        CodegenConfig config = CodegenConfigLoader.forName(lang);
        System.out.println("CONFIG OPTIONS");
        for (CliOption langCliOption : config.cliOptions()) {
            System.out.println("\t" + langCliOption.getOpt());
            System.out.println("\t    "
                    + langCliOption.getOptionHelp().replaceAll("\n", "\n\t    "));
            System.out.println();
        }
    }
}
