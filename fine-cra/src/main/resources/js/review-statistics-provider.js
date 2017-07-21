/**
 * 检查结果数据提供者，rest请求检查数据，若成功返回true，并刷新页面，显示prAnalyzerStatisticsPanel
 */
define('fr/cra-review-statistics', [
    'jquery',
    'aui',
    'underscore',
    'bitbucket/util/navbuilder',
    'bitbucket/util/server',
    'bitbucket/internal/feature/pull-request/can-merge',
    'bitbucket/util/state',
    'exports'
], function ($, AJS, _, navBuilder, server, canMerge, state, exports) {
    'use strict';

    function getRepoStatisticsUrl() {
        return navBuilder.rest('cra', '1.0').addPathComponents('reviewstatistics').build();
    }

    function refreshStatisticsPanel(context) {
        $('#cra4fr-statistics').html(com.fr.cra.prAnalyzerStatisticsPanel(context));
        if (state.getPullRequest().state === 'OPEN') {
            canMerge(); // re-trigger merge check
        }
    }

    function getParams(context) {
        return {
            'pullRequestId': context.pullRequest.id,
            'repoId': context.pullRequest.toRef.repository.id
        };
    }

    exports.showCodeReviewStatistics = function (context) {
        server.poll({
            url: getRepoStatisticsUrl(),
            pollTimeout: 15 * 60 * 1000, // 15 min
            interval: 2000, // 2 sec
            data: getParams(context),
            tick : function (data, textStatus, xhr) {
                if (xhr.status === 201) { // no content (yet)
                    refreshStatisticsPanel({});
                } else if (xhr.status === 200 && data) {
                    refreshStatisticsPanel({ statistics: data });
                    return true; // success, stop polling
                } else if (xhr.status === 202) {
                    refreshStatisticsPanel({ statistics: data, notUpToDate: true });
                } else if (xhr.status >= 400 && xhr.status <= 599) { // error
                    refreshStatisticsPanel({ error: xhr.responseText });
                    return false; // failure, stop polling
                } else {
                    refreshStatisticsPanel({});
                }
                // keep polling. return undefined is implied.
            },
            statusCode : {
                400 : function (xhr, textStatus, errorThrown) {
                    refreshStatisticsPanel({ error: xhr.responseText });
                    return false; // do not do any default handling
                },
                500 : function (xhr, textStatus, errorThrown) {
                    refreshStatisticsPanel({ error: xhr.responseText });
                    return false; // do not do any default handling
                }
            }
        });
        return {};
    };

    //判断数据是否激活，
    exports.isCodeReviewStatisticsActive = function(context) {
        var isStatisticsActive = false;
        server.rest({
            async: false,
            url: getRepoStatisticsUrl(),
            data: getParams(context),
            statusCode: {
                '200': function () {
                    isStatisticsActive = true;
                    return false; // don't handle this globally.
                },
                '201': function () {
                    isStatisticsActive = true;
                    return false; // don't handle this globally.
                },
                '202': function () {
                    isStatisticsActive = true;
                    return false; // don't handle this globally.
                },
                '204': function () {
                    isStatisticsActive = false;
                    return false; // don't handle this globally.
                },
                '400': function () { // we want to show the error in the context provider function above
                    isStatisticsActive = true;
                    return false; // don't handle this globally.
                }
            }
        });
        return isStatisticsActive;
    };

});