/*
 * Copyright (c) 2015 by Mibex Software GmbH, Zurich, Switzerland.
 *
 * All rights reserved.
 *
 * This product is the proprietary and sole property of Mibex Software GmbH.
 * Use, duplication or dissemination is forbidden and is subject to prior
 * written consent of Mibex Software GmbH (office@mibex.ch).
 */

define('fr/cra-pullrequest-templates', [
        'jquery',
        'aui',
        'underscore',
        'bitbucket/util/events',
        'bitbucket/util/state',
        'bitbucket/util/navbuilder',
        'bitbucket/util/server',
        'aui/flag',
        'exports'
    ],
    function ($, AJS, _, events, state, navBuilder, server, flag, exports) {
        'use strict';

        function initPullRequestDescription() {
            events.on('bitbucket.internal.widget.commitsTable.contentAdded', function () {
                server.rest({
                    type: 'GET',
                    url: navBuilder
                        .rest('cra', '1.0')
                        .addPathComponents('pullrequest-templates')
                        .build(),
                    data: {
                        repoId: state.getRepository().id
                    },
                    statusCode: {
                        200: function (result) {
                            $('#pull-request-description').val(result.responseText);
                        },
                        204: function (result) {
                            // not enabled, nothing to do
                            return false;
                        },
                        400: function (result) {
                            flag({
                                type: 'error',
                                title: 'Code Review Assistant: failed to initialize pull request template feature',
                                persistent: false,
                                body: result.responseText
                            });
                            return false;
                        }
                    }
                });
            });
        }

        exports.onReady = function () {
            initPullRequestDescription();
        };

    }
);

jQuery(document).ready(function () {
    require('fr/cra-pullrequest-templates').onReady();
});
