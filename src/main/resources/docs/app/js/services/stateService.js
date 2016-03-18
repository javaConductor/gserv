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

/**
 * Created by lcollins on 7/22/14.
 */
/**
 * Created by lcollins on 6/28/14.
 */
(function(module){
    module.factory('StateService', function($rootScope,  storage, $location) {
        var stateService = {};
        stateService.stateFunctions = {}
        var currentPath = $location.path()

        stateService.hasPreviousState = function(path) {
            return ! (! storage[path])
        };

        stateService.save = function(path, data){
            storage[path] = data
        }
        stateService.restore = function(path){
            return storage[path];
        }


        /**
         *
         * @param path
         * @param saveFn()
         * @param restoreFn( storedObject )
         */
        stateService.initialize = function(path, saveFn, restoreFn){
            //var path = $location.path()
            var pathFunctions = this.stateFunctions[path] || {};
            pathFunctions.save = saveFn;
            pathFunctions.restore = restoreFn;

            var data = storage[path] || {};
            if(this.hasPreviousState(path)){
                restoreFn(data)
            }
        }

        var onDestroy = function(path){
              var fns = stateService.stateFunctions[path];
            if(fns && fns.save){
                storage[path] = fns.save();
            }
        }

        $rootScope.$on("$locationChangeSuccess", function () { onDestroy(currentPath); });
        //$rootScope.$on("$destroy", function () { onDestroy($location.path()); });
        return stateService;
    })
})(angular.module('stateServices', ['angularLocalStorage'] ) );
