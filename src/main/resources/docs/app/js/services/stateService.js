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
