Kanso = {};
Kanso.makeLink = function(url, label){
	return $("<a href='"+ url +"'>" + label + "</a>");
};

Kanso.views = ["#introduction", "#impression", "#search"];
Kanso.nowView = "#introduction";
Kanso.lastView = "#introduction";
Kanso.fadeSpeed = 100;

Kanso.toggleView = function(target){
	if(target !== Kanso.nowView){
		Kanso.lastView = Kanso.nowView;
		Kanso.nowView = target;
	
		$.each(Kanso.views, function(i, v){
			if(v === target){
				$(v).show(100);
			} else {
				$(v).hide(100);
			}
		});
	}
};

Kanso.back = function(){
	Kanso.toggleView(Kanso.lastView);
};

Kanso.checkInitialLocation = function(){
	var hash = document.location.hash;
	if(hash.indexOf("impression") !== -1){
		var key = hash.split(/\//)[2];
		Kanso.showImpression(key);
	} else if(hash.indexOf("tag") !== -1){
		var tag = hash.split(/\//)[2];
		Kanso.showTagBooks(tag);
	}
};


Kanso.showTagLinks = function(tagArr){
	var bookTag = $("#book_tag");
	bookTag.html("");
	$.each(tagArr, function(i, v){
		var link = Kanso.makeLink("/#!/tag/" + v, v);
		link.bind("click", function(){
			Kanso.showTagBooks(v);
		});
		bookTag.append(link);
		bookTag.append(",");
	});
};

Kanso.showImpression = function(impKeyStr){
	$("#impression_content").show();
	$("#get_other_impressions").show();
	$("#other_impressions").hide();

	Kanso.toggleView("#impression");
	$.getJSON("/impression", {key: impKeyStr}, function(res){
		$("#book_title").html(res.title);

		var tagArr = $.map(res.tag, function(v){ return v.tag; });
		var tagText = tagArr.join(", ");

		Kanso.showTagLinks(tagArr);
		$("#new_tag").val(tagText);
		$("#impression_user").html(res.username);
		$("#impression_date").html(res.date);
		$("#impression_text").html(res.text);

		$("#impression").show(100);
		Kanso.showingBookKeyStr = res.parentkey;
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

// =parts {{{
Kanso.loadLogin = function(){
	$.getJSON("/parts/login", {}, function(res){
		if(res.flag){
			$("#login_user").html(res.name);
			$("#login_form").hide();
			$("#login_link").hide();
			$("#logout_link").show();
		}
	});
};

Kanso.loadSecretQuestions = function(){
	var target = $(".load_secret_questions");
	target.append("<option>読み込み中</option>");
	$.getJSON("/parts/secret_questions", {}, function(res){
		target.html("");
		$.each(res, function(i, v){
			target.append("<option value='"+i+"'>"+v+"</option>");
		});
	});
};
// }}}

$(function(){
	Kanso.checkInitialLocation();

	Kanso.loadLogin();
	Kanso.loadSecretQuestions();

	$("#login_link").show();
	$("#auth").css("position", "relative");
	$("#login_form").css("position", "absolute").css("right", "0").hide();
	$("#login_link a").bind("click", function(){
		$("#login_form").toggle(Kanso.fadeSpeed, function(){
			$("#login_form input[type=password]").focus();
		});

	});

	$.getJSON("/list", {}, function(res){
		var ul = $("#recent_list ul");
		ul.html("");
		$.each(res, function(i, v){
			var anchor = $("<a href='/#!/impression/"+ v.keystr + "'>"+ v.title +" ("+ v.username +")</a>");
			anchor.bind("click", function(){
				Kanso.showImpression(v.keystr);
			});
			ul.append($("<li></li>").append(anchor));
		});
	});

	$("a.back").bind("click", Kanso.back);

	$("a#edit_tag").bind("click", function(){
			$("#edit_tag").hide();
			$("#book_tag").hide(100, function(){
				$("#new_tag").bind("keypress", function(e){
					if(e.charCode === 13){
						var newtag = $(e.target).val();
						$.post("/update_tag", {key: Kanso.showingBookKeyStr, tag: newtag}, function(savedTagJsonStr){
							Kanso.showTagLinks($.parseJSON(savedTagJsonStr));
							$("#book_tag").show();
							$("#new_tag").hide();
							$("#edit_tag").show();
						});
					}
				}).show();
			});
	});

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

//	$("input#search").bind("keypress", function(e){
//		if(e.charCode === 13){
//			var keyword = $(e.target).val();
//			$.post("/search")
//		}
//	});

});
