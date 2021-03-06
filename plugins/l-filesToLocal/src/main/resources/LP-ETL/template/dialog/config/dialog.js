define([], function () {
    "use strict";

    const DESC = {
        "$namespace" :
            "http://plugins.linkedpipes.com/ontology/l-filesToLocal#",
        "$type": "Configuration",
        "path" : {
            "$type" : "str",
            "$property" : "path",
            "$control": "pathControl",
            "$label" : "Target path"
        }
    };

    function controller($scope, $service) {

        if ($scope.dialog === undefined) {
            $scope.dialog = {};
        }

        const dialogManager = $service.v1.manager(DESC, $scope.dialog);

        $service.onStore = function () {
            dialogManager.save();
        };

        dialogManager.load();

    }

    controller.$inject = ['$scope', '$service'];
    return controller;
});
