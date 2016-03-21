/*
 *  The MIT License (MIT)
 *
 *  Copyright (c) 2014-2016 Lee Collins
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in
 *  all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

'use strict';

angular.module('gservDocsApp')
    .controller('MainController', ['$location', '$scope', '$rootScope', 'AlertService',
        function ($location, $scope, $rootScope, alertService) {

            $scope.menuItems = [
                {
                    title: "GServ Documentation",
                    view: 'docs/index.html',
                    children: [
                        {
                            title: "GServ API",
                            view: 'docs/api/index.html',
                            children: []
                        }, {
                            title: "GServ Usage",
                            view: 'docs/usage/index.html',
                            children: []
                        }, {
                            title: "GServ Plugins",
                            view: 'docs/plugins/index.html',
                            children: []
                        }
                    ]
                }
            ]

            $scope.gotoPage = function (page) {
                $location.path(page)
            }
            $scope.errorMessages = []
            $scope.menu = [
                {text: 'HOME', href: '#/'},
                {text: 'SEARCH', href: '#search'},
                {text: 'VERSES', href: '#verses'}
            ];

            $scope.closeAlert = function (idx) {
                alertService.closeAlert(idx);
            }

        }]);
