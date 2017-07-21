/**
 * 取出配置文件的信息
 */

(function ($) {
    var builtinAnalyzerNames = [ "Checkstyle", "PMD" ];
    /**
     * 处理checkstyle／pmd 三个radio button
     */
    function registerRadioSelectWhenUserInputClick() {
        function getNameOfRadioButton($el) {
            return $el.parent().parent().find('input:radio').attr('name');
        }

        for (var i = 0; i < builtinAnalyzerNames.length; i++) {
            $('#' + builtinAnalyzerNames[i] + 'ConfigUrl').click(function() {
                $("input[name=" + getNameOfRadioButton($(this)) + "][value=FROM_URL]").prop('checked', true);
            });
            $('#' + builtinAnalyzerNames[i] + 'ConfigRepoPath').click(function() {
                $("input[name=" + getNameOfRadioButton($(this)) + "][value=FROM_REPO]").prop('checked', true);
            });
        }
    }

    $(document).ready(function () {
        registerRadioSelectWhenUserInputClick();
    });

})(AJS.$);