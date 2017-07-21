define('fr/cra-review-statistics/toggle-analysis', [
    'jquery',
    'aui',
    'underscore',
    'bitbucket/util/events',
    'bitbucket/util/state',
    'bitbucket/util/navbuilder',
    'bitbucket/util/server',
    'exports'
    ],
    function ($, AJS, _, events, state, navBuilder, server, exports) {
        'use strict';

        function runAnalysisNow() {
            showWaitingCursor();

            server.rest({
                type: 'POST',
                url: navBuilder.rest('cra', '1.0').addPathComponents('toggle-analysis', 'run-analysis').build(),
                data: {
                    repoId: state.getRepository().id,
                    pullRequestId: state.getPullRequest().id
                },
                success: function (data) {
                    hideWaitingCursor();
                    window.location.reload();
                },
                statusCode: {
                    400: function (response) {
                        hideWaitingCursor();
                        return true;
                    },
                    500: function (response) {
                        hideWaitingCursor();
                        return false;
                    }
                }
            });
        }

        function removeComments() {
            showWaitingCursor();
            server.rest({
                type: 'POST',
                url: navBuilder.rest('cra', '1.0').addPathComponents('toggle-analysis', 'remove-comments').build(),
                data: {
                    repoId: state.getRepository().id,
                    pullRequestId: state.getPullRequest().id
                },
                success: function (data) {
                    hideWaitingCursor();
                    window.location.reload();
                },
                statusCode: {
                    400: function (response) {
                        hideWaitingCursor();
                        return true;
                    },
                    500: function (response) {
                        hideWaitingCursor();
                        return false;
                    }
                }
            });
        }

        function bindButtons() {
            $('.finecra-run-analysis-button').mousedown(runAnalysisNow);
            $('.finecra-remove-comments-button').mousedown(removeComments);
        }

        function showWaitingCursor() {
            $("html").addClass("cra-wait");
        }

        function hideWaitingCursor() {
            $("html").removeClass("cra-wait");
        }

        exports.onReady = function () {
            bindButtons();
        };

    }
);

jQuery(document).ready(function () {
    require('fr/cra-review-statistics/toggle-analysis').onReady();
});


