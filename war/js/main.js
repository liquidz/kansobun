Kanso = {};
Kanso.makeLink = function(url, label){
	return $("<a href='"+ url +"'>" + label + "</a>");
};

//Kanso.views = ["#introduction", "#impression", "#search"];
//Kanso.nowView = "#introduction";
//Kanso.lastView = "#introduction";
Kanso.nowScreen = "#new_user";
Kanso.lastScreen = Kanso.nowScreen;
Kanso.toggleSpeed = 100;
Kanso.impressionShortTextLimit = 5;
Kanso.space = function(){ return $("<span>").html("&nbsp;").addClass("space"); };

Kanso.show = function(id, fn){
	console.log("showing: " + id);
	if(Mia.isNull(fn)){
		$(id).show(Kanso.toggleSpeed);
	} else {
		$(id).show(Kanso.toggleSpeed, fn);
	}
};
Kanso.hide = function(id, fn){
	console.log("hiding: " + id);
	if(Mia.isNull(fn)){
		$(id).hide(Kanso.toggleSpeed);
	} else {
		$(id).hide(Kanso.toggleSpeed, fn);
	}
};

Kanso.toggleScreen = function(id, fn){
	if(id !== Kanso.nowScreen){
		var hoge = $(Kanso.nowScreen);
		console.log(hoge);
		hoge.hide(Kanso.toggleSpeed, function(){
			$(id).show(Kanso.toggleSpeed, function(){
				Kanso.lastScreen = Kanso.nowScreen;
				Kanso.nowScreen = id;

				if(! Mia.isNull(fn)){ fn(); }
			});
		});
//		Kanso.hide(Kanso.nowScreen, function(){
//			Kanso.show(id, function(){
//				Kanso.lastScreen = Kanso.nowScreen;
//				Kanso.nowScreen = id;
//				if(! Mia.isNull(fn)){ fn(); }
//			});
//		});
	}
};

Kanso.setLoadingGif = function(target){
	$(target).html("<img src='/img/ajax-loader.gif' alt='loading' />");
};

Kanso.back = function(){ Kanso.toggleScreen(Kanso.lastScreen); };

Kanso.checkInitialLocation = function(){
//	var hash = document.location.hash;
//	if(hash.indexOf("impression") !== -1){
//		var key = hash.split(/\//)[2];
//		Kanso.showImpression(key);
//	} else if(hash.indexOf("tag") !== -1){
//		var tag = hash.split(/\//)[2];
//		Kanso.showTagBooks(tag);
//	}
};


Kanso.makeTagLinks = function(tags){
	var span = $("<span>");

	$.each(tags, function(i, v){
		Kanso.makeLink("/#!/tag/" + v.tag, v.tag)
			.bind("click", function(){ Kanso.showTagBooks(v.tag); })
			.appendTo(span);
	});

	return span;
};

Kanso.showImpression = function(impKeyStr){
	//$("#impression_content").show();
	//$("#get_other_impressions").show();
	//$("#other_impressions").hide();

	//Kanso.toggleView("#impression");
	$.getJSON("/impression", {key: impKeyStr}, function(res){

		$("#impression .title").html(res.title);
		$("#impression .tag").html("").append(Kanso.makeTagLinks(res.tag));
		$("#impression .user").html(res.username);
		$("#impression .date").html(res.date);
		$("#impression .text").html(res.text);

		Kanso.show("#impression");
		//Kanso.showingBookKeyStr = res.parentkey;
	});
};

Kanso.showTagBooks = function(tag){
	Kanso.toggleView("#search");
	var books = $("#search_result");
	books.html("");
	$.getJSON("/tag", {name: tag}, function(res){
		console.log(res);
		$.each(res, function(i, v){
			books.append("<p>"+v.title+"</p>");
		});
	});
};

Kanso.makeAvatar = function(impression){ // {{{
	var res = null;

	var src = "http://www.gravatar.com/avatar/"

	if(impression.usename !== "guest" && !Mia.isBlank(impression.mailmd5)){
		res = $("<img>").attr("src", "http://www.gravatar.com/avatar/" + impression.mailmd5);
	} else {
		res = $("<img>").attr("src", "/img/noavatar.png");
	}
	return res.attr("alt", "avatar").addClass("avatar");
}; // }}}

Kanso.cutText = function(text){
	return ((text.length >= Kanso.impressionShortTextLimit)
			? (text.substr(0, Kanso.impressionShortTextLimit) + "...")
			: text);
};

Kanso.makeImpressionLinks = function(impression){
	var li = $("<li>");

	Kanso.makeLink("/#!/impression/" + impression.keystr, impression.title)
		.addClass("impression")
		.bind("click", function(){ Kanso.showImpression(impression.keystr); })
		.appendTo(li);

	Kanso.space().appendTo(li);
	$("<span>")
		.addClass("user")
		.append(Kanso.makeAvatar(impression))
		.append(Kanso.makeLink("/#!/user/" + impression.username, impression.username))
		.appendTo(li);

	return li.after($("<p>").addClass("text").html(Kanso.cutText(impression.text)));
};

// =getRecentImpressionList
Kanso.getRecentImpressionList = function(){
	var ul = $("#recent_impressions ul");
	Kanso.setLoadingGif(ul);
	$.getJSON("/impressions", {}, function(res){
		ul.html("");
		$.each(res, function(i, v){
			//var anchor = $("<a href='/#!/impression/"+ v.keystr + "'>"+ v.title +" ("+ v.username +")</a>");
			//anchor.bind("click", function(){
			//	Kanso.showImpression(v.keystr);
			//});
			//ul.append($("<li></li>").append(anchor));
			ul.append(Kanso.makeImpressionLinks(v));
		});
	});
};

Kanso.showUserImpressions = function(name, page){
	$.getJSON("/impressions", {user: name, page: page}, function(res){
		$.each(res, function(i, v){

		});
	});
};


Kanso.initLoginForm = function(){
	$("#login_link").show();
	$("#auth").css("position", "relative");
	$("#login_form").css("position", "absolute").css("right", "0").hide();
	$("#login_link a").bind("click", function(){
		$("#login_form").toggle(Kanso.toggleSpeed, function(){
			$("#login_form input[type=password]").focus();
		});
	});
};

Kanso.loadLogin = function(){ // {{{
	$.getJSON("/parts/login", {}, function(res){
		if(res.flag){
			$("#login_user").html(res.name);
			$("#login_form").hide();
			$("#login_link").hide();
			$("#logout_link").show();

			// toggle screen to recent impression if user logged in
			Kanso.toggleScreen("#recent_impressions");
		}
	});
}; // }}}

Kanso.loadMessage = function(){
	$.getJSON("/parts/message", {}, function(msg){
		$("#message").html(msg);
	});
};

Kanso.loadSecretQuestions = function(){ // {{{
	var target = $(".load_secret_questions");
	target.append("<option>読み込み中</option>");
	$.getJSON("/parts/secret_questions", {}, function(res){
		target.html("");
		$.each(res, function(i, v){
			target.append("<option value='"+i+"'>"+v+"</option>");
		});
	});
}; // }}}

$(function(){
	Kanso.checkInitialLocation();

	Kanso.initLoginForm();
	Kanso.loadLogin();
	//Kanso.loadSecretQuestions();
	Kanso.getRecentImpressionList();
	Kanso.loadMessage();

	//$("a.back").bind("click", Kanso.back);

	//$("a#edit_tag").bind("click", function(){
	//		$("#edit_tag").hide();
	//		$("#book_tag").hide(100, function(){
	//			$("#new_tag").bind("keypress", function(e){
	//				if(e.charCode === 13){
	//					var newtag = $(e.target).val();
	//					$.post("/update_tag", {key: Kanso.showingBookKeyStr, tag: newtag}, function(savedTagJsonStr){
	//						Kanso.showTagLinks($.parseJSON(savedTagJsonStr));
	//						$("#book_tag").show();
	//						$("#new_tag").hide();
	//						$("#edit_tag").show();
	//					});
	//				}
	//			}).show();
	//		});
	//});

	$("a#get_other_impressions").bind("click", function(){
		$.getJSON("/impressions", {key: Kanso.showingBookKeyStr}, function(res){
			$("#impression_content").hide();
			$("#get_other_impressions").hide();
			var target = $("#other_impressions");
			target.html("");
			$.each(res, function(i, v){
				var dl = $("<dl></dl>");
				dl.append("<dt>"+v.username+" ("+v.date+")</dt>");
				var dd = $("<dd><pre>"+v.text+"</pre></dd>");
				//var link = Kanso.makeLink("javascript:void(0);", "この感想の感想");
				//link.bind("click", function(){
				//	console.log("comment to " + v.keystr);
				//});
				//dd.append(link);
				dl.append(dd);
				target.append(dl);
			});
			target.show();
		});
	});

	$("a.nav").bind("click", function(e){
		var target = $(e.target);
		var id = target.attr("href").split(/\//)[2];
		Kanso.toggleScreen("#" + id, function(){
			// toggle .selected
			$("nav ul li.selected").removeClass("selected");
			$("nav ul li a[href=/#!/"+id+"]").parent().addClass("selected");

			// optional
			if(id === "recent_impressions"){
				Kanso.getRecentImpressionList();
			} else if(id === "recent_users"){
				Kanso.getRecentUserList();
			} else {
				console.log("sonota: " + id);
			}
		});
	}).bind("mouseover", function(e){
		$(e.target).parent().animate({paddingBottom: "10px"}, 100);
	}).bind("mouseout", function(e){
		$(e.target).parent().animate({paddingBottom: "5px"}, 100);
	});

	$("a.back").bind("click", Kanso.back);

//	$("input#search").bind("keypress", function(e){
//		if(e.charCode === 13){
//			var keyword = $(e.target).val();
//			$.post("/search")
//		}
//	});

});
