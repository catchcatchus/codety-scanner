package io.codety.scanner.reporter;

import io.codety.scanner.reporter.dto.CodeAnalysisResultSetDto;
import io.codety.scanner.reporter.sarif.SarifResultReporter;
import io.codety.scanner.reporter.slack.SlackResultReporter;
import io.codety.scanner.service.CodeAnalysisResultFilterService;
import io.codety.scanner.service.CodeChangeDiffAnalyzer;
import io.codety.scanner.service.dto.AnalyzerRequest;
import io.codety.scanner.reporter.console.ConsoleResultReporter;
import io.codety.scanner.reporter.github.GithubPullRequestResultReporter;
import io.codety.scanner.util.CodetyConsoleLogger;
import io.codety.scanner.util.CodetyConstant;
import io.codety.scanner.prework.dto.GitFileChangeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CodeAnalysisResultDistributionService {

    @Autowired
    CodeAnalysisResultFilterService codeAnalysisResultFilterService;
    @Autowired
    CodeChangeDiffAnalyzer codeChangeDiffAnalyzer;
    @Autowired
    ConsoleResultReporter consoleResultReporter;
    @Autowired
    GithubPullRequestResultReporter githubPullRequestResultReporter;

    @Autowired
    SlackResultReporter slackResultReporter;

    @Autowired
    SarifResultReporter sarifResultReporter;

    public boolean distributeAnalysisResult(AnalyzerRequest analyzerRequest, CodeAnalysisResultSetDto codeAnalysisResultSetDto) {

//        CodetyConsoleLogger.debug("Before filtering: " + codeAnalysisResultSetDto.toString());
        if (analyzerRequest.isFilterByGitDiff()) {
            GitFileChangeList gitFileChangeList = codeChangeDiffAnalyzer.getGitDiffFileChanges(analyzerRequest.getExternalPullRequestMergeTargetBranchRef(), analyzerRequest.getLocalGitRepoPath());
            if (gitFileChangeList.isValid()) {
                codeAnalysisResultFilterService.filterAnalysisResultForOnlyChangedCode(codeAnalysisResultSetDto, gitFileChangeList);
                //            CodetyConsoleLogger.debug("After filtering: " + codeAnalysisResultSetDto.toString());
                CodetyConsoleLogger.info(CodetyConstant.INFO_FINISHED_RESULT_FILTERING);
            } else {
                CodetyConsoleLogger.info(CodetyConstant.INFO_SKIP_RESULT_FILTERING);
            }
        } else {
            CodetyConsoleLogger.debug(CodetyConstant.INFO_SKIP_RESULT_FILTERING);
        }



        consoleResultReporter.deliverResult(analyzerRequest, codeAnalysisResultSetDto);

        githubPullRequestResultReporter.deliverResult(analyzerRequest, codeAnalysisResultSetDto);

        slackResultReporter.deliverResult(analyzerRequest, codeAnalysisResultSetDto);

        sarifResultReporter.deliverResult(analyzerRequest, codeAnalysisResultSetDto);

        return true;
    }

}
