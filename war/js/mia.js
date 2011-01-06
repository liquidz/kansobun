(function(){
	if(!window.Mia){ Mia = {}; }

	Mia.isNull = function(x){ return(x === undefined || x === null); };
	Mia.isBlank = function(x){ return(Mia.isNull(x) || x === ""); };

	// http://16c.jp/2008/0528214632.php
	Mia.scope = function(ns, fn){ return function(){ return fn.apply(ns, arguments); }; };
})();
