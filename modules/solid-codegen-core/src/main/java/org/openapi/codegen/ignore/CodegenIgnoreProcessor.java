package org.openapi.codegen.ignore;

import org.openapi.codegen.ignore.rules.DirectoryRule;
import org.openapi.codegen.ignore.rules.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Presents a processing utility for parsing and evaluating files containing common ignore patterns. (.solid-codegen-ignore)
 */
public class CodegenIgnoreProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CodegenIgnoreProcessor.class);

    private File ignoreFile = null;

    private List<Rule> exclusionRules = new ArrayList<>();
    private List<Rule> inclusionRules = new ArrayList<>();

    /**
     * Loads the default ignore file (.solid-codegen-ignore) from the specified path.
     *
     * @param baseDirectory The base directory of the files to be processed. This contains the ignore file.
     */
    public CodegenIgnoreProcessor(final String baseDirectory) {
        this(baseDirectory, ".solid-codegen-ignore");
    }

    /**
     * Loads the specified ignore file by name ([ignoreFile]) from the specified path.
     *
     * @param baseDirectory The base directory of the files to be processed. This contains the ignore file.
     * @param ignoreFile    The file containing ignore patterns.
     */
    @SuppressWarnings("WeakerAccess")
    public CodegenIgnoreProcessor(final String baseDirectory, final String ignoreFile) {
        final File directory = new File(baseDirectory);
        final File targetIgnoreFile = new File(directory, ignoreFile);
        if (directory.exists() && directory.isDirectory()) {
            loadFromFile(targetIgnoreFile);
        } else {
            LOGGER.warn("Output directory does not exist, or is inaccessible. No file (.solid-codegen-ignore) will be evaluated.");
        }
    }

    /**
     * Constructs an instance of {@link CodegenIgnoreProcessor} from an ignore file defined by {@code targetIgnoreFile}.
     *
     * @param targetIgnoreFile The ignore file location.
     */
    public CodegenIgnoreProcessor(final File targetIgnoreFile) {
        loadFromFile(targetIgnoreFile);
    }

    private void loadFromFile(File targetIgnoreFile) {
        if (targetIgnoreFile.exists() && targetIgnoreFile.isFile()) {
            try {
                loadCodegenRules(targetIgnoreFile);
                this.ignoreFile = targetIgnoreFile;
            } catch (IOException e) {
                LOGGER.error(String.format("Could not process %s.", targetIgnoreFile.getName()), e.getMessage());
            }
        } else {
            // log info message
            LOGGER.info(String.format("No %s file found.", targetIgnoreFile.getName()));
        }
    }

    void loadCodegenRules(final File codegenIgnore) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(codegenIgnore))) {
            String line;

            // NOTE: Comments that start with a : (e.g. //:) are pulled from git documentation for .gitignore
            // see: https://github.com/git/git/blob/90f7b16b3adc78d4bbabbd426fb69aa78c714f71/Documentation/gitignore.txt
            while ((line = reader.readLine()) != null) {
                if(
                    //: A blank line matches no files, so it can serve as a separator for readability.
                    line.length() == 0
                ) continue;

                Rule rule = Rule.create(line);

                // rule could be null here if it's a COMMENT, for example
                if(rule != null) {
                    if (Boolean.TRUE.equals(rule.getNegated())) {
                        inclusionRules.add(rule);
                    } else {
                        exclusionRules.add(rule);
                    }
                }
            }
        }
    }

    /**
     * Determines whether or not a file defined by {@code toEvaluate} is allowed,
     * under the exclusion rules from the ignore file being processed.
     *
     * @param targetFile The file to check against exclusion rules from the ignore file.
     * @return {@code false} if file matches any pattern in the ignore file (disallowed), otherwise {@code true} (allowed).
     */
    public boolean allowsFile(final File targetFile) {
        if(this.ignoreFile == null) return true;

        File file = new File(this.ignoreFile.getParentFile().toURI().relativize(targetFile.toURI()).getPath());
        Boolean directoryExcluded = false;
        Boolean exclude = false;
        if(exclusionRules.size() == 0 && inclusionRules.size() == 0) {
            return true;
        }

        // NOTE: We *must* process all exclusion rules
        for (int i = 0; i < exclusionRules.size(); i++) {
            Rule current = exclusionRules.get(i);
            Rule.Operation op = current.evaluate(file.getPath());

            switch (op){
                case EXCLUDE:
                    exclude = true;

                    // Include rule can't override rules that exclude a file by some parent directory.
                    if(current instanceof DirectoryRule) {
                        directoryExcluded = true;
                    }
                    break;
                case INCLUDE:
                    // This won't happen here.
                    break;
                case NOOP:
                    break;
                case EXCLUDE_AND_TERMINATE:
                    i = exclusionRules.size();
                    break;
            }
        }

        if(exclude) {
            // Only need to process inclusion rules if we've been excluded
            for (int i = 0; exclude && i < inclusionRules.size(); i++) {
                Rule current = inclusionRules.get(i);
                Rule.Operation op = current.evaluate(file.getPath());

                // At this point exclude=true means the file should be ignored.
                // op == INCLUDE means we have to flip that flag.
                if(op.equals(Rule.Operation.INCLUDE)) {
                    if(current instanceof DirectoryRule && directoryExcluded) {
                        // e.g
                        // baz/
                        // !foo/bar/baz/
                        // NOTE: Possibly surprising side effect:
                        // foo/bar/baz/
                        // !bar/
                        exclude = false;
                    } else if (!directoryExcluded) {
                        // e.g.
                        // **/*.log
                        // !ISSUE_1234.log
                        exclude = false;
                    }
                }
            }
        }

        return Boolean.FALSE.equals(exclude);
    }

    /**
     * Allows a consumer to manually inspect explicit "inclusion rules". That is, patterns in the ignore file which have been negated.
     *
     * @return A unmodifiable {@link java.util.List} of rules which possibly negate exclusion rules in the ignore file.
     */
    public List<Rule> getInclusionRules() {
        return Collections.unmodifiableList(inclusionRules);
    }

    /**
     * Allows a consumer to manually inspect all "exclusion rules". That is, patterns in the ignore file which represent
     * files and directories to be excluded, unless explicitly overridden by {@link CodegenIgnoreProcessor#getInclusionRules()} rules.
     *
     * NOTE: Existence in this list doesn't mean a file is excluded. The rule can be overridden by {@link CodegenIgnoreProcessor#getInclusionRules()} rules.
     *
     * @return A unmodifiable {@link java.util.List} of rules which define exclusions by patterns in the ignore file.
     */
    public List<Rule> getExclusionRules() {
        return Collections.unmodifiableList(exclusionRules);
    }
}
