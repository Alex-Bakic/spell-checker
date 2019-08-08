# <p align="center"> A guide to the Clojure Spec library </p>

The clojure.spec library was introduced in the `1.9.0` version of the language. Born out of a recognition that documentation , of individual functions and collective behaviours , was not adequate for complex systems using clojure. This may have something to do with dynamic typing , being succinct as it is , the compiler does not have an exhaustive amount of information to work with. Furthermore, without the use of type hints and annotations it is up to the programmer to write extensive, well-thought-out unit tests to allow complex projects to survive.

However this is argubly not the principle of clojure, to provide all these things , and that the true abstraction itself comes from the data that we organise and manipulate. To truly reap the rewards of using lisp , namely the flexibility of homoiconicity.

Spec is sort of like our battle-armour. It's a strong outer layer that wraps around us, stopping ourselves from geting injured unnecessarily from puny foes. Or to put it bluntly, spec provides checks around our defined data. Each spec, the checks we define , are the set of allowed rules or values that data passed to a spec must uphold. Specs can be defined through predicates, sets or other composed of other specs, and the functions available in the `clojure.spec.alpha` library. It aims to be able to provide verification to the underlying properties and to uphold only those properties we care about. It certainly is more flexible than a traditional type system as we can do anything from uphold the value of a single key in a map to defining specs that uphold entire types.

Data verification isn't all that spec can do for us though. As specs are just rules, just clojure functions, they can be used to perform generative-testing. Example data is generated based on the specs we define (all the properties of our data we want to be generated), which is incredibly powerful as it produces a larger number of wider-ranged, more expressive tests. This is a good way of building upon docstrings, as they can't really be leveraged by programs or other testing facilities as all they can do is project information to the human consumer. Spec is a rarity in this sense as it is effectively allowing the computer and the programmer to understand the domain of the software without having to mention ad-hoc conventions that work for one party or only the other.

To recap, what we must define are the specs themselves. We define the properties to be upheld, so then we can use the validation functions that `clojure.spec.alpha`, `clojure.spec.gen.alpha` and `clojure.spec.test.alpha` provides. 

So, to get familiar with spec let's hop into a repl:

      ;; require the spec library
      ;; note , you must have a version of clojure that has a version of 1.9.0 or above
      (require '[clojure.spec.alpha :as s])
      
Now the validation functions at our disposal differ slightly. I mentioned earlier that specs *uphold* certain properties of that data, and spec will do this by either *conforming* the data or checking it is *valid* or *invalid*. The difference is subtle, being valid concerns itself with whether the data simply passes the checks, yielding true or false, whereas conform, upon passing the checks, may also mould the data depending on it's properties and characteristics, by destructuring or formatting the data in some way .Let's start by using the `valid?` function on some example data.

    user=> (doc s/valid?)
    -------------------------
    clojure.spec.alpha/valid?
    ([spec x] [spec x form])
      Helper function that returns true when x is valid for spec.
    nil
    
    (s/valid? zero? 0)
    true
    
    (s/valid? zero? 1)
    false
    
We can pass any valid predicate to it really:

    ;; it just checks that the data itself can pass the tests, by invoking the "spec"
    user=> (s/valid? #{:a :b :c} :c)
    true
    user=> (s/valid? #{:a :b :c} :d)
    false

Alright, now for an actual example. We'll make a file `core.clj` in the folder `hello_world`.

    (ns hello-world.core 
      (:require [clojure.spec.alpha :as s]
                [clojure.string :refer [ends-with?]]))
              
    (defn punctuated? [s]
       "check if a string ends with a full-stop , question or exclamation mark."
       (let [punctuation #{"." "?" "!"}]
        (some (partial ends-with? s) punctuation)))
    
    (s/valid? punctuated "Hello World!") ;; => true
    (s/valid? punctuated "Hello World") ;; => false
    
Now, we might want to know how exactly has our spec failed, and with more ambiguous functions especially so! In this case could use the `s/explain` function in an attempt to find out what is going wrong.

    ;; just pass it the spec and the arguments to it and it will return some data on the evaluation
    ;; all operations to *out* return nil as IO is a side effect operation
    (s/explain punctuated? "Hello World") 
    "Hello World" - failed: punctuated?
    nil 
    
Well, that's about as much as we know about the error. What gives? Well, one way of getting a more in-depth look at the results of our evaluation would be to the use `s/explain-data` function:

    (s/explain-data punctuated? "Hi")
    ;; #:clojure.spec.alpha{:problems [{:path [], :pred user/punctuated?, :val "Hi", :via [], :in []}], 
    ;; :spec #object[user$punctuated_QMARK_ 0x6f1c3f18 "user$punctuated_QMARK_@6f1c3f18"], :value "Hi"}

This is better, but it is a bit cluttered. This is because we haven't registered the spec globally. Moreover, it deals with the spec as a normal clojure function, but by *def*ining this as a spec we can get a slightly clearer report.

Keep in mind this isn't the main reason we define specs. While this is a nice add-on, we want to define specs so we can reuse them across our library or application. By using `s/def` we give it a name , typically an auto-resolved keyword like `::punctuated?` and then our spec. This globally defines the spec and it adds it to the central registry for which we can now reuse specs across our application. The clojure reader will resolve it to a fully qualified namespace, so in our case it would be `:hello-world.core/punctuated?` . If we are writing code for our own use, or within a company, then it won't hurt to use these simpler conventions; if we are writing libraries for public use the we should take into account possible conflicts, and include things like , but not limited to : project name, url, organisation name etc. 

But for brevity, and the fact this is a little project, I'm going to use auto-resolved keywords. 

    (s/def ::punctuated? punctuated?)
    ;; now it's defined, it allows functions like doc to print some info on it:
    
    ;; (doc ::punctuated?)
    ;; =>  :hello-world.core/punctuated?
    ;;     Spec
    ;;         punctuated?
    ;;     nil


    ;; and now if we try and use s/explain
    (s/explain ::punctuated? "Hello World")
    ;; => "Hello World" - failed: punctuated? spec: :hello-world.core/punctuated?
    
    ;; how about s/explain-data ?
    ;; #:clojure.spec.alpha{:problems [{:path [], :pred hello-world.core/punctuated?, :val "Hi", 
    ;; :via [:hello-world.core/punctuated?],  :in []}], :spec :user/punctuated?, :value "Hi"}

Some differences to note. the `:via` keyword's value vector is populated with where the spec came from, as it is now
catalogued in our registry. Also the `:spec` keyword is no longer a load of jargon. It's on the whole, a lot more readable. 

Side note: the difference between `:val` and `:value` is that `:value` is everything that was passed into the spec whereas `:val` is the part of the input , the value, that failed. So the two could differ if we provide things like collections.

On the whole this extended, clearer report is of marginally greater use. 

But this *isn't* the fault of spec. 

This is a very simple function. It narrowes down the number of culprits to one as there is only one function. Complexity is so minimal that spec can't really produce a more detailed report as we're only checking one property of the function. Of course it is important that with each predicate, that only one property is checked with it. As all our checks are pure functions, we can compose them very simply. And this is where the fun starts and spec becomes more useful.

So, we're happy we can check that a sentence is punctuated, but we would also like to check that a given sentence starts with a capital letter. 

    (ns hello-world.core 
      (:require [clojure.spec.alpha :as s]
                [clojure.string :refer [ends-with? ]]))
              
    (defn punctuated? [s]
       "check if a string ends with a full-stop , question or exclamation mark."
       (let [punctuation #{"." "?" "!"}]
        (some (partial ends-with? s) punctuation)))
    
    (defn starts-with-capital? [s]
      "Check if a given word , or sentence , starts with a capital letter."
      (Character/isUpperCase (first s)))
      
    (s/def ::punctuated? punctuated?)
    (s/def ::starts-with-capital? starts-with-capital?)
    
Now we can use the `s/and` function to check if a given input is valid by both specs.

    (s/def ::proper-sentence? (s/and ::punctuated? ::starts-with-capital?))

    (s/valid? ::proper-sentence? "Hello there")
    ;; => false
    (s/valid? ::proper-sentence? "hello there!")
    ;; => false
    (s/valid? ::proper-sentence? "Hello there!")
    ;; => true

We can make new specs from smaller ones, using their names. We don't need to use the functions directly, as it is more expressive, and the specs themselves may comprise other specs. Specs are not only composable, but re-usable. We can use the `::starts-with-capital?` spec for validating a noun, as they enforce the same rules. 

    (s/def ::noun? ::starts-with-capital?)
    
Now before we jump into an example with `s/or`, it is important to note that `s/or`'s functionality is a little different from `s/and`. The latter will just take spec forms, one after another and return true or false on whether all of them were matched. However `s/or` on the other hand, if given three specs for example and a given input only returns true for one of them then that is enough to suffice a pass (that *or* this *or* that). Meaning that for `s/or` to fail, every given spec must fail, so make sure each spec enforces a single property that you're validating. Otherwise the `s/explain-data` report will be of less use.

This is why when we use it, for every spec (property) we include a key that names each check so that `s/or` can easily show which properties the input passed on. Let's continue this grammatical theme and introduce a set of functions that can check what *kind* of a sentence has been entered. Either, they are continuing an idea with "Moreover", summarising a paragraph with "In conclusion" etc.

    ;; all in the file writing.clj , but I'll just reference a couple fns
    
    (ns spell-checker.writing
      (:require [clojure.spec.alpha :as s]
                [clojure.spec.test.alpha :as stest]
                [clojure.spec.gen.alpha :as gen]
                [clojure.string :refer [includes?]]))

    ;; for the purpose of this exercise the sets are extremely simple...
    (defn good-addition?
      [sentence]
      (some (partial includes? sentence) #{"Furthermore" "Therefore" "In addition" "Moreover"}))
      
    (defn bad-addition?
      [sentence]
      (some (partial includes? sentence) #{"And" "The" "Because"}))
      
    (s/def ::good-addition good-addition?)
    (s/def ::bad-addition bad-addition?)
    (s/def ::check-addition (s/or :good ::good-addition? :bad ::bad-addition?))

Now I am going to contradict some of my previous advice here, as you can see that I will be reusing a lot of the functionality that are in the predicate functions, but just changing the sets around. You could just group them and do a single `and` check along the lines of `(and (good-set word) (nil? (bad-set word)))`. But it depends on the complexity, and how much you prioritise testing on a particular part of your program.

Now it would be nice to see the type of sentence that were passed to us, whether indicative of the start or the end of a paragraph or essay.But using valid isn't a good test as we want to see WHICH property passed and what kind of data it was. 

    user=> (s/valid? ::check-addition? "Furthermore")
    true
    
But what if we want to see the data that got returned, what *kind* of sentence is it? Well, this is where the `s/conform` function comes in. Conform will take the data and mould it into the shape of our spec. 

When we use it in this case, we get back:

    user=> (s/conform ::check-addition? "Furthermore")
    [:addition-of-idea "Furthermore"]
    
    user=> (s/conform ::check-summary? "Clearly")
    [:summary "Clearly"]
    
We can actually see the type of sentence, and the data itself can be used as the input to other functions. `s/conform` , in simple cases , will just return the data it was passed if there is no reformatting of destructuring of any kind. If the spec fails it will return the `:clojure.spec.alpha/invalid` keyword.

    user=> (s/conform ::starts-with-capital? "Hello")
    "Hello"
    user=> (s/conform ::starts-with-capital? "ello")
    :clojure.spec.alpha/invalid
    
So let's look at an example, where all of our specs fail:

    user=> (s/valid? ::good-opener? "Furthermo")
    false

    user=> (s/explain-data ::good-opener? "Furthermo")
    #:clojure.spec.alpha{:problems
                           ({:path [:addition-of-idea],
                            :pred spell-checker.core/good-addition?,
                            :val "Furthermo",
                            :via [:spell-checker.core/good-opener?],
                            :in []}
                           {:path [:example-signalling],
                            :pred spell-checker.core/good-example?,
                            :val "Furthermo",
                            :via [:spell-checker.core/good-opener?],
                            :in []}
                           {:path [:summary],
                            :pred spell-checker.core/good-summary?,
                            :val "Furthermo",
                            :via [:spell-checker.core/good-opener?],
                            :in []}),
                          :spec :spell-checker.core/good-opener?,
                          :value "Furthermo"}

As not a single spec succeeded we'll get back every spec in the `s/explain-data` report. If it doesn't meet any criteria, then in order for `s/conform`to return something useful (that other functions in the pipeline could work with) we could have a "last-resort spec" which just returns an empty map or some basic default settings. For example, if we are reading from the database and our spec which checks for data returns nil then we should have another spec which just returns an empty map. Then conform could return that, and other functions in this pipeline wouldn't have to worry about nil-objects. 

    user=> (s/conform (s/or :pass ::check-addition? :fail identity) "Furthermo")
    [:fail "Furthermo"] 

#### Note , using s/nilable to wrap around our specs to accept nil values does not help the overall pipeline. As conform would still return nil which isn't good for other functions that may have expected some sort of map.

However it is debatable whether this sort of thing is actually useful in a production environment. Maybe you do want to let it crash, maybe you do want to show a nil value. Such things though are great for testing, if you are doing many tests at once, to see the value that fails instead of the `:clojure.spec.alpha/invalid` keyword.

Now then, that about covers our introduction to the very basics of spec. I haven't really covered anything, but that's because I wanted you to get used to spec's philosophy and it's raison d'être. We're going to expand on this kind of spelling theme by introducing the [Words API](https://www.wordsapi.com/#try). Given a word it can provide us with it's synonyms, antonyms , definitions and much more. But now our little program is letting in all sorts of data from the outside world! To keep things solid we will need to define some specs. Just some of the issues we could have is sending a request with a misspelled word and getting back an error, we might call the wrong function but because of their similar response format it may slip through.

I won't be going through the actual implementation of the http client as I want to keep things focused on spec, but you can check it out [here](https://www.github.com/Alex-Bakic/spell-checker/src/spell_checker/handler.clj). But nevertheless,  these are the main functions we will expose and their respective annotations, so we know what our result should come back as.

    synonyms - takes a word and makes a request for all it's synonyms
    *the-word* -> {"word" *the-word* "synonyms" [all the synonyms]}

    antonyms - takes a word and makes a request for all it's antonyms
    *the-word* -> {"word" *the-word* "antonyms" [all the antonyms]}

    definitions - takes a word and makes a request for all it's definitions
    *the-word* -> {"word" *the-word* "definitions" [{"definition" "the-definition" "part-of-speech" "..."}]}

     examples - takes a word and show example usages in a sentence or phrase
    *the-word* -> {"word" *the-word* "examples" ["the-example"]}

    rhymes - takes a word and makes a request for all it's rhyming words.
    *the-word* -> {"word" *the-word* "rhymes" {"all" [all the rhymes]}}

    syllables - takes a word and shows the number of syllables
    *the-word* -> {"word" *the-word* "syllables" {"count" the-count "list" [all the syllables]}}
   
    frequency - takes a word and shows how common the word is on a scale of 1 - 10.
    *the-word* -> {"word" *the-word* "frequency" {"zipf" 1<=x<=10 "per-million" number "diversity" 0<=x<=1}}
   
And if we provide a word that isn't in the dictionary we get back a stacktrace error, with the HTTP request. In the body we would get `{"success" false "message" "word not found"}` so we need to make sure that we check the word is in the dictionary before the request is issued. 

Ok so let's construct the spec for checking words
    
    (require '[clojure.string :refer [split-lines]])
    
    (defn load-dictionary
      []
      (-> (slurp "src/words.txt")
                 (split-lines)
                 (set)))

    (def dictionary (load-dictionary))

    ;; check that a word is in the dictionary
    ;; all the words in our text file are lower case so it won't pass unless the other two checks pass.
    (defn in-dictionary?
      [word]
      (dictionary word))

    (defn all-lower-case? 
      [word]
      (every? #(Character/isLowerCase %) word))
      
And to check a word is valid. 

    (require '[spell-checker.dictionary :refer [dictionary]])
   
    (s/def ::dictionary dictionary)
    (s/def ::word ::dictionary)
   
We don't have to do any other checks, because if it is in the dictionary then it is correct. No need to add `string?` , `isLowerCase` and with spec allowing sets as predicate checks we're left with a very nice solution (albeit to just one little portion of the api). 

As we're defining an API it is only right we start following some idioms. For the betterment of the code and the consumer. Instead of just leaving it be at a fully resolved keyword, we should be specific:

    ;; we can still use the auto-resolve sugar on the second spec, as it is evaluated to the first kw.
    (s/def :spell-checker.dictionary/dictionary dictionary)
    (s/def :spell-checker.dictionary/word ::dictionary)
       
    ;; even better to do add the company name, then the application if applicable
    ;; (s/def :com.something.spell-checker.dictionary/word ::dictionary)

Aside from this, when I was making requests to the words API , in the response map the values , as well as the keys, were strings and not (un)qualified keywords. Now we shall see that when we try to check the map keys/values with a helpful function called `s/keys` it doesn't work with strings, only (un)qualified keywords. We use `s/keys` to desribe what we want our map to look like, typically with fully qualified keywords `::like-this-one` as the keys and that will be used as the spec for the corresponding value.

For `s/keys` to describe the map, one way we could do it is to transform the keys the api returns from strings to keys beforehand by utilising the `clojure.walk/keywordize-keys` function, which can take the incoming response map and have all the keys formatted into keywords. 

Now then it's time to inspect the specifics. The simplest functions to validate are the `synonyms`, `antonyms` , and `examples` set of functions which return a simple structure to work with. Where they differ from other functions is that some will use maps , others will use vectors. 

    ;; calling the synonyms function, which you can see in the handler.clj file
    (synonyms "garish")
    
    ;; returns , with the keywordizing-keys applied to it:
    {:word "garish",
     :synonyms
            ["brassy"
             "cheap"
             "flash"
             "flashy"
             "gaudy"
             "gimcrack"
             "loud"
             "meretricious"
             "tacky"
             "tatty"
             "tawdry"
             "trashy"]}

Looking at what we need to define here, the `:word` key stays the regardless of any function that is called so we can re-use it: 

    ;; same spec as above, s/keys will use the spec name as the name of the key it expects to be passed to it.
    (s/def ::word dictionary)
    
Now as the value of the synonyms key is a vector, and we want to check every value is a string, we can use the `s/coll-of` spec to say we want a collection of strings, each one being different, and the kind of collection we want to check is a vector.
    
    ;; check that the value with the synonyms key is a vector of strings
    (s/def ::synonyms (s/coll-of string? :distinct true :kind vector?))
    
    (s/valid? ::synonyms ["brassy" "cheap" "flash" "gaudy" ....])
    ;; => true
    
And this would be how we could spec the entire map adding them together.

    (s/def ::check-synonyms (s/keys :req-un [::word ::synonyms))
                                    
So the `:req-un` keyword means all the required keys that the map *must* have. The "un" on the end means the required key in question can be an unqualified keyword, like the ones we get back from the API, as otherwise we would have to restructure our whole app to allow for namespaced keywords. 

Subsequently, the `opt-un` key is for specifiying all the optional keys a map *could* have. So if we had wished to make the spec a bit more modular we could have done:

    (s/def ::check-spec (s/keys :req-un [::word]
                                :opt-un [::synonyms ::antonyms ::rhymes ::examples]))
    
    ;; would also pass if we include a :synonyms key as well
    ;; any combination of optional keys would pass, which would be all wrong.
    (s/valid? ::check-spec {:word "garish",
                            :antonyms
                              ["brassy"
                              "cheap"
                              "flash"
                              "flashy"
                              "gaudy"
                              "gimcrack"
                              "loud"
                              "meretricious"
                              "tacky"
                              "tatty"
                              "tawdry"
                              "trashy"]})
    ;; => true
    
However this isn't a great idea as it will no doubt amplify stacktraces and impede your ability to find the actual failing the spec if there are mixture of maps that were supplied. It's better to keep it simple and avoid cases like the one above. However, if we were going to go down this route and clobber all of our checks under one name a better solution, which only allows one optional key present is to use `or` , not `s/or` , but `or` within `opt-un`. This is because opt-un matches against all the keywords that are given, by supplying `or` it does the matching at that level, returning the only match left inside the `opt-un` vector.

    (s/def ::check-spec (s/keys :req-un [::word]
                                :opt-un [(or ::synonyms ::antonyms ::rhymes ::examples)]))
                                
    
    (s/valid? ::check-spec {:word "garish" :antonyms ["some" "antonym"] :synonyms ["some" "synonym"]})
    ;; => false
    
This is certainly an option if you wanted modularity within your specs, to vary depending on the incoming data, but spec has a cleaner, more extensible solution to this sort of problem. The solution we will cover a little later. As of right now, the plan will be to have the `::word` spec and specs like `::synonyms` under a joint roof like `::check-synonyms` , and that will be the convention. Just need to spec the other keys now, next is the syllables function.

    (syllables "condescending")

    ;; returns
    {:word "condescending", :syllables {:count 4, :list ["con" "de" "scend" "ing"]}}

    ;; since specs are added to a global registry, I can specify them below...
    (s/def ::syllables (s/keys :req-un [::count ::list]))
    
    (s/def ::count int? #(> % 0))
    
    (s/def ::list (s/coll-of string? :kind vector?))
    
    ;; and to check the entire map
    (s/def ::check-syllables (s/keys :req-un [::word ::syllables]))
    
And now the frequency function...

    (frequency "monad")
    
    ;; returns
    {:word "monad", :frequency {:zipf 1.9, :perMillion 0.07, :diversity 0}}
    
    ;; so to describe the value of the :frequency key we can do
    (s/def ::frequency (s/keys :req-un [::zipf ::perMillion ::diversity]))
    
    (s/def ::zipf (s/and number? #(<= % 10)))
    (s/def ::perMillion (s/and number? #(<= % 1000000)))
    (s/def ::diversity (s/and float? #(<= 0 % 1)))

    (s/def ::check-frequency (s/keys :req-un [::word ::frequency]))

Now if we run this as it is with "monad" we get back an error. Let's take a look at what `explain-data` has to say:

    (s/explain-data ::check-frequency frequency-result)
     #:clojure.spec.alpha{:problems
                          ({:path [:frequency :diversity],
                            :pred clojure.core/float?,
                            :val 0,
                            :via
                            [:spell-checker.handler/check-frequency
                             :spell-checker.handler/frequency
                             :spell-checker.handler/diversity],
                            :in [:frequency :diversity]}),
                          :spec :spell-checker.handler/check-frequency,
                          :value
                          {:word "monad",
                           :frequency {:zipf 1.9, :perMillion 0.07, :diversity 0}}}

So we can see the issues are to do with the `::diversity` spec, the error is that while we accept any number including 0 or 1, 0 is not treated as float , which is fair enough. While it would be nice to keep the `float?` check, since we are doing the additional check of `#(<= 0 % 1)` we can get away with `number?`

     ;; so what we are saying is, unless it is a zero or a one, check it is a float,
     ;; within the range of zero to one inclusive, which allows the or check to pass too.
     (s/def ::diversity (s/and number? #(<= 0 % 1)))
     
And with that it's time to take on the loch-nested monster. Bring on the `definitions` function. 

    (definitions "monad")
    
    ;; returns
    {:word "monad",
     :definitions
       [{:definition
          "a singular metaphysical entity from which material properties are said to derive",
        :partOfSpeech "noun"}
        {:definition
        "(biology) a single-celled microorganism (especially a flagellate protozoan)",
         :partOfSpeech "noun"}
        {:definition "(chemistry) an atom having a valence of one",
         :partOfSpeech "noun"}]}

So this is the first and only time we've really come across nested collections. `s/keys` made it simple, as the collection was separated almost by the key, but with the vector we have no such comfort, unless we hack away at working with indexes.

What we can do here is use the `s/coll-of` function, that holds a spec containing all the checks we would perform on every map, using `s/keys`. We will discuss the other ways in a moment.

     
     (s/def ::check-definitions (s/keys :req-un [::word ::definitions]))
     
     ;; since it's a vector we don't have any keys that may constrict our naming
     (s/def ::definitions (s/coll-of ::each-definition))
     
     (s/def ::each-definition (s/keys :req-un [::definition ::partOfSpeech]))
     
     (s/def ::definition string?)
     
     (s/def ::partOfSpeech #{"noun" "pronoun" "adjective" "adverb" "proverb" "verb"
                                            "preposition" "conjunction" "interjection"})
                                            
The trick with these kind of collections is to break it down, and define the layers. So we start at the very top level keys which are `:word` and `:definitions`. Now word is , as always , very straightforward and we can chalk it off at just `string?` , but when we check the `:definitions` key we will have a vector of maps. Now given the vector, the spec we can use to go a layer further would be `s/coll-of`. Taking a look at the docs of `coll-of` we can see that it will exhaustively conform every value given to it, so coll-of will look at each value in the vector, in this case a map, and feed it to the spec. This brings us to our next layer, which is a map. Now we know what to do here from previous specs we have written, just model our ideal map, with the required (and optional) keys and define their constraints. `partOfSpeech` is a bit more interesting as it uses a set to make sure that any value outside is rejected.  This data was little harder than other nested collections, as each layer had a different set of requirements. We had to deal with a map , then a vector , than a map with different keys and values, meaning that we couldn't reuse much and had to define new layers as we went along. 

Now let's write the spec for the `word` function, which is essentially when we don't call a specific function like `synoyms`, but get back as much as possible of any and all sorts of data.The data follows the exact same style as the `::definitions` spec, but each map in the vector contains an arbitrary amount of keys on all the different properties of the given word, for a given definition. 

What we can do is reuse the spec `::each-definition` as it has the default keys which every map should contain, namely `::word` and `::definition`. But we also want to include the plethora of optional keys that could be present, which is a perfect time to use `s/merge` to create an `s/keys` spec which reflects all this:

     (s/merge
         ::each-definition
         (s/keys :req-un [] :opt-un [::examples ::derivation ::typeOf
                                     ::similarTo ::entails ::inCategory]))

And then we can wrap all this in an `s/coll-of` for checking every map this way.

    (s/def ::results (s/coll-of
                       (s/merge
                         ::each-definition
                         (s/keys :req-un [] :opt-un [::examples ::derivation ::typeOf
                                                     ::similarTo ::entails ::inCategory]))))

So that has the vector covered, for the `::results` key but there are other top level keys we need to spec like `::frequency`, `::pronunciation` , `::syllables`. At first glance I thought I could just reuse the `::frequency` spec I defined earlier but it deals with the frequency format that comes as a map. What we can do is utilise the `s/or` function to wrap a number-format into it, so we can work with either. 

    ;; before
    (s/def ::frequency
      (s/keys :req-un [::zipf ::perMillion ::diversity])))

    ;; after
    (s/def ::frequency
       (s/or :number-format (s/and number? #(<= 0 % 10))
             :map-format (s/keys :req-un [::zipf ::perMillion ::diversity])))

So we know the benefits of doing this: reusability, flexibility and succintness. But the downsides are only going to be realised further down the road. When you do this, we get test cases that pass when they otherwise shouldn't as they belonged to that of a different request. Moreover a number could be the key in a corresponding frequency request which wouldn't be right in that case, but still returning a pass. In this case it's important to clarify which frequency is meant for which type of request.

    (s/def :frequency/frequency
        (s/keys :req-un [::zipf ::perMillion ::diversity]))

    (s/def :word/frequency
        (s/and number? #(<= 0 % 10)))
        
So now it is much clearer to someone reading the spec where and what the purpose of it is. Remembering that spec is also supposed to function as a supplement to documentation and seeing the `or` spec above can confuse, in which cases will it be a "number-format", and where is this used? But the name actually shows where this is referenced, whilst keeping the frequency name for `s/keys` to perform checks.

And so the spec for checking the word should look like:

    (s/def ::check-word
       (s/keys :req-un [::word ::results ::syllables ::pronunciation :word/frequency]))
       
Now let's start to include some of this checks into our production code, what we could do is add some pre and post checks to our request function. So before sending the http request, we would see if the word given to us is present in our dictionary and after the request check that we got back some sort of response map.

    (defn request 
      [[header-name header-value] param word]
      {:pre [(s/valid? :spell-checker.dictionary/word word)]
       :post [(s/valid? map? %)]}
      (let [url "https://wordsapiv1.p.rapidapi.com/words/"
            req (str url word "/" param)
            options {:headers {header-name header-value}
                     :accept :json}]
        (client/get req options)))
        
      
However this in no way solves our issue of checking the characterisitics of the response map, why'd we bother writing out all those specs? Well, we've got all the properties we want to define, so what we could do instead is put the `pre` and `post` map verifications in each api function, so `::check-rhymes` as the `post` condition in the `rhymes` function etc.

But is there a better way? What spec offers some APIs is the option to use `s/multi-spec`. For APIs that communicate using a unique indentifier, so for example a key that is common in all maps of possible requests, but has differing values (attributes) which would point to the correct method. `s/multi-spec` acts as a wrapper around the `defmulti` and `defmethod` functions , leveraging their polymorphism and putting our specs in their method bodies.

If you're familiar with multimethods you know that they do this based on a dispatch function which will return a dispatch tag, which is the value that dictates which method is the right one to use.

    ;; adapted example taken from doing (doc s/multi-spec)
    
    ;; just like the regular way of setting up defmulti we provide a function, we're being very succint
    ;; and leveraging the power of keywords to be able to pull whatever value in the map is at the :tag key.
    
    ;; this keyword can be resolved or un-resolved, but it's what must be seen in the data passed when multi-spec is called.
    ;; otherwise we can't pull out the value at that entry.
    ;; In this case the identifier is an un-resolved keyword , that should be in every map. 
    (defmulti mspec :tag)

    ;; methods can ignore their argument, as s/multi-spec is called with the data
    ;; once the value is found and the correct method determined , the method will return the spec
    ;; so now whether we are using conform or valid the data and the spec will be checked.
    (defmethod mspec :int [_] (s/keys :req-un [::tag ::i]))
    
    ;; careful not to confuse this with :tag up above at defmulti, this is just the spec used for the map
    (s/def ::tag keyword?)
    
    (s/def ::i number?)

    ;; this is our wrapper. We use the *same tag* that our defmulti does, but this is simply to avoid errors down the line,
    ;; which I'll explain in the paragraph below.
    ;; multi-specs , for their second argument can take either a keyword or a function, 
    ;; the difference between the two will become more apparent when we do generative testing...
    (s/def ::mmspec (s/multi-spec mspec :tag))
    
    (s/valid? ::mmspec {:tag :int :i 4})
    ;; => true
    
Using the `:tag` key is very succint , but it may cause some confusion. `defmulti` will just take a function like normal , and using the return value work out which method. `:tag` itself could've been replaced with the more verbose `#(first (vals %))` , just as long as we are identifying the right property.

    ;; these two are functionally equivalent for maps of the form {:tag keyword? :i :number?}
    (defmulti mspec :tag)
    (defmulti mspec #(first (vals %)))
    
Moving now to `s/multi-spec` itself now , if you've never seen or used it before this is the part where you mind gets blown! For you see, the second argument of `s/multi-spec` **is only important for generating values, testing . That's it.** In fact, if you weren't doing any generation (which I'll cover further down in the article), just using `s/valid?` , `s/conform`  then you could just use `nil` and carry on...

    ;; the second arg, retag is only necessary for generation. nil won't change how the underlying
    ;; defmulti and methods work, as they are used simply to deduce , from the map , which is the right 
    ;; spec to hand over.
    (s/def ::mmspec (s/multi-spec mspec nil))
    
    (s/valid? ::mmspec {:tag :int :i 4})
    ;; => true
    
    (s/conform ::mmspec {:tag :int :i 4})
    ;; => {:tag :int :i 4}
    
The only reason you would would want to *not* have nil, is for the case of generating samples and checking that your specs do in fact pass the right properties, and so you would want to have the same key used so it passes the `s/keys` check. Now then, let's take a look at the example used in the [clojure spec guide](https://clojure.org/guides/spec#_multi_spec) and see if we can leverage this in our own spell-checker program. We see that `:event/type` is the omnipresent identifier, the key in every map, looking at our program, we don't have that sort of format. The only key that stays the same for us is `:word` but we can't define methods for every word in the dictionary! At least manually... 
 
We needed an identifier that persists in all requests *and* give us information on the "type" of data we have, so taking our usual map:

     ;; instead of this,
     {:word "Some word" :synonyms ["some" "synonyms"]}
     
     ;; we'd need this for multimethods
     {:type :synonyms :word "some-word" :synonyms ["some" "synonyms"]}
     
     ;; then s/multi-spec would look for a spec with :synonyms and that should work.
     ;; We do repeat ourself slightly, but if we wanted to use multimethods into this API
     ;; then this is the simplest way of doing it.
     
To implement this it would be just another wrapper in the `wrap-request` function, grabbing the second key and assoc'ing to the map for spec to check it.

     ;;
     ;; in our handler.clj file
     ;;
     
     (defn- show-type
       [m]
       (assoc m :type (second (keys m))))
    
    (defn- request
      [[header-name header-value] param word]
      (let [url "https://wordsapiv1.p.rapidapi.com/words/"
            req (str url word "/" param)
            options {:headers {header-name header-value}
                     :accept :json}]
        (client/get req options)))
    
    (defn- format-response
      [[header-name header-value] param word]
      (-> (request [header-name header-value] param word)
          (json->clj)
          (keywordize-keys)
          (show-type)))
    
    ;;
    ;; in our handler_spec.clj file
    ;;
    
    ;; key we expect in every map
    (defmulti spell-checker :type)

    ;; all the values , and given the data , the spec we would like to run.
    (defmethod spell-checker :synonyms [_] (s/keys :req-un [::type ::word ::synonyms]))
    (defmethod spell-checker :antonyms [_] (s/keys :req-un [::type ::word ::antonyms]))
    (defmethod spell-checker :rhymes [_] (s/keys :req-un [::type ::word ::rhymes]))
    (defmethod spell-checker :syllables [_] (s/keys :req-un [::type ::word ::syllables]))
    (defmethod spell-checker :frequency [_] (s/keys :req-un [::type ::word ::frequency]))
    (defmethod spell-checker :definitions [_] (s/keys :req-un [::type ::word ::definitions]))
    (defmethod spell-checker :word [_] (s/merge (s/keys :req-un [::type]) ::check-word))
    
    ;; the spec for the type key itself, could use a set to define the only accepted vals...
    (s/def ::type keyword?)
    
    ;; and we'll use the below to put into each api function , around about this :  {:pre [...] :post [(s/valid? handler-spec/response %)}

    ;; keeping :type in for when we test this out later on
    (s/def ::response (s/multi-spec spell-checker :type))
     
    (s/valid? :spell-checker.handler-spec/response {:type :antonyms :word "strong" :antonyms ["weak" "feeble"]})
    ;; => true
    
We may in the future want to add more methods but we have to amend the spec, on the whole though it reduces the number of things we have to think about; However in cases where you have to restructure your api to fit this, you may find that it means you end up writing a lot of similar code. Whether you need all that is up to you. 

In conclusion, think about whether your API would benefit from including this layer of abstraction and whether the API itself can cleanly accomodate it.

# Testing our functions , using clojure.spec.gen.alpha and clojure.spec.test.alpha

Testing functions is slightly different from standard value checks, as we can make calls to foreign api's (as we will do in a moment) , there could be cases where we need to spec higher order functions which means another layer of complexity. Moreover, `s/valid?` and `s/conform` aren't enough for testing. What we can use is the `exercise-fn` validator as well as the tools in the `clojure.spec.test.alpha` namespace.

We'll define functions specs using `s/fdef` which is what is says on the tin, a definition for a spec, tailored to functions. We can define specs for things like the arguments through `:args` and the return type through `:ret`. The properties of the function, the distinct features that link the input and output, wherever possible, can be specified through the `:fn` keyword, all of this allowing us to see whether the function would work before we ship it to production.

We include the function name to the spec so it is registered to the appropriate fn, meaning when we do (doc my-fn) it will show the specs too this means the name is not an (un)qualified keyword, because the fn name isn't, and the registry we will match the specs for us. We reference arguments our actual function takes by keywordizing the names, so `:word` for word,  so if we were to write an fdef for the rhymes function:
     
     (s/fdef rhymes :args (s/cat :word ::word) :ret ::check-rhymes :fn '#(= (:word %) :word))
     
     ;; note the :fn keyword essentially says , "take out the word in the map" 
     ;; using the :word key and compare it to the :word parameter.
     
     ;; it is important to use some sort of sequence spec wrapper  for :args so you can specify the 
     ;; actual number of arguments, if you don't all the arguments will be put into one spec.
     ;; it should be one spec per argument, but of course you can use s/and to compose.
     
     ;; you could also use s/and outside of the sequencing function, meaning you would describe
     ;; the positioning of the arguments as well as any characteristic of the argument(s)
     ;; themselves. 
     
     ;; :args is given a map , so if we did (rhymes "silly") it would be converted to 
     ;; {:word "silly"}
     (s/fdef rhymes :args (s/and (s/cat :word ::word) #(not= (:word %) "orange")) 
                    :ret ::check-rhymes)
                    
I'll use all the specs that check the rhyme function:

    ;; all the specs that we will use for the rhymes function
    (s/def :rhymes/all (s/coll-of string? :kind vector? :distinct true))
    (s/def :rhymes/empty (s/and map? empty?))
    (s/def ::rhymes (s/or :results (s/keys :req-un [:rhymes/all])
                          :empty :rhymes/empty))
   
    (s/def ::check-rhymes (s/keys :req-un [::word ::rhymes]))

    (s/fdef rhymes :args (s/cat :word ::word)
                   :ret ::check-rhymes)
                   
Now when we do use `s/exercise-fn`, it will make actual HTTP requests and it will use the words in our own dictionary as the main input. The only problem is some words in our dictionary *aren't words in their dictionary*. Nonetheless this can make our errors harder to reason about, so in spec we trust that it is them and not us.
                 
    ;; use the ` here as exercise-fn expects the symbol, which it uses to grab
    ;; the spec from the registry
    (s/exercise-fn `rhymes 2)
    ;; this will return anything , but when I tried it =>
    ([("naphtha")
      {:word "naphtha",
       :rhymes
       {:all
         ["naphtha"
          "petroleum naphtha"
          "shale naphtha"
          "solvent naphtha"
          "wood naphtha"]}}]
      [("navet") {:word "navet", :rhymes {}}])

Instead of writing a function definition manually for each one, I think it would be rather nice to abstract it all out into a macro. Then we can use the macro, to dynamically generate tests, and use `exercise-fn` to generate words and call them to see if they pass.

    (defn param->spec
      "given a param, returns the appropriate spec check for that request."
      [param]
      (let [specs {"synonyms" ::check-synonyms
                   "antonyms" ::check-antonyms
                   "definitions" ::check-definitions
                   "frequency" ::check-frequency
                   "syllables" ::check-syllables
                   "rhymes" ::check-rhymes
                   nil ::check-word}]
        (specs param)))

    (defmacro param->fdef
      [param]
      (list 's/fdef (symbol param)
             :args (s/cat :word ::word)
             :ret (param->spec param)
             :fn '#(= (:word %) :word)))

We can leverage the naming conventions of our parameters and our api functions to generate a complete function definition for a given function. We can call `param->fdef` with say "synonyms" and it will add it to the global registry for the synonyms function, viewable by `doc`.

    (param->fdef "synonyms")
    => spell-checker.handler/synonyms
     
    (doc synonyms)
    -------------------------
    spell-checker.handler/synonyms
    ([word])
    Spec
      args: (cat :word :spell-checker.handler/word)
      ret: (keys :req-un [:spell-checker.handler/word :spell-checker.handler/synonyms])
      fn: (= (:word %) :word)
    nil
    
    (s/exercise-fn `synonyms 5)
    ([("sheepskins")
      {:word "sheepskin", :synonyms ["diploma" "lambskin" "parchment" "fleece"]}]
     [("herbert") {:word "herbert", :synonyms ["victor herbert"]}]
     [("cardamine") {:word "cardamine", :synonyms ["genus cardamine"]}]
     [("ranchman") {:word "ranchman", :synonyms []}]
     [("nonphilosophical") {:word "nonphilosophical", :synonyms []}])
    
Now that we have described the properties of our function, namely the `:args`. `:ret` and `:fn` properties, we can use some of the wrappers in the `clojure.spec.test.alpha` namespace to enfore them. `instrument` for example is a function, that when called, will "set the var's root binding to a fn that checks arg conformance (throwing an exception on failure) before delegating to the original fn" - as stated by the docs. 

    (require '[clojure.spec.test.alpha :as stest])
    
    ;; do (doc "fn-name") to see if there is already a spec present
    (doc synonyms)
    
    -------------------------
    spell-checker.handler/synonyms
    ([word])
    nil
    
This means that when we do `fdef` or , use `param->fdef` it needs to be done in the same namespace, otherwise the link doesn't form and it will expect there is a function called synonyms defined in whatever arbitrary namespace we happen to be in. This is why it can be handy to put all the functions and specs in the same namespace, so things like this don't happen. All we need to do though, if is switch into a repl and load the `spell-checker.handler` namespace, then `require` things like `param->fdef` and we are ready to get started.
    
    ;; so, assuming the appropriate namespace,
    (param->fdef "synonyms")
    ;; => spell-checker.handler/synonyms
    
    (stest/instrument `synonyms)
    ;; => [spell-checker.handler/synonyms]
    ;; which confirms to us it has been found and linked.
    
    ;; otherwise we would be handed an empty vector
    
Now if we want to call synonyms, it will validate the arguments before calling the real fn itself.

    (synonyms 3)
    ;; invalid argument to spell-checker.handler/synonyms ....
    ;; Spec assertion failed.
    ;; at [:word] :spec :spell-checker/handler-spec/dictionary
    
    (synonyms "functional")
    => {:word "functional" :synonyms  ["operable" "operational" "usable" "useable" "operative" "running" "working"]}
    
While this is nice, it's only leveraging a third of our specification. How about testing the `:ret` and `:fn` namspaces, well the next step up would be the `stest/check` function, which is similar to the `exercise-fn` we saw earlier, but is more thorough and comes with a host of different options. But to be honest, all we need to do is call it with the symbol and watch the magic happen. 

I just tried it now, and it went past twenty or so options, and I realised I was making a mistake with the `:fn` property of the `fdef` spec, as it passes a map of {:args "the-arg" , :ret "the-ret-val"} and I was too simplistic by just wanting to just pull the `:word` out as it is in the value of those keys. So I changed it, and abstracted it out into a separate fn:

    (defn fn-check
      "Is passed the value that :fn would from fdef,
      for all our api functions that would be:

      {:args {:word \"the-word\"} , :ret {:word \"daphnis\"}}

      Given this I would take the value of the word in :args and the
      value of the word in :ret and check for equality.
     "
    [conformed-map]
    (let [args-word (:word (:args conformed-map))
          ret-word (:word (:ret conformed-map))]
      (= args-word ret-word)))
      
Then I can stick that into the `:fn` part of my `param->fdef` macro and we can catch cases for where it does end up happening. For example, a word that I never would have thought of in a million years "jemadars" (meaning a minor official or junior officer) is actually tweaked to the singular. In the rare cases that the word is different between the two maps I may add a clause for things like plurals. 

An incredibly useful tool, is the `stest/enumerate-namespace` function. What this does is , you give it a namespace and it proceeds to grab every symbol that names vars in that ns, chucking them into a set to be passed to something like `stest/check` to do many functions at once. Now this doesn't actually define `fdef`'s, so to give each function it's own spec I would have to keep calling `param->fdef`, then call `stest/enumerate-namespace` , passing the result to `stest/check` giving me a complete rundown of each and every API function. 

     (stest/enumerate-namespace `spell-checker.handler)
     #{spell-checker.handler/rhymes spell-checker.handler/antonyms
      spell-checker.handler/defaults spell-checker.handler/*api-key*
      spell-checker.handler/json->clj spell-checker.handler/syllables
      spell-checker.handler/examples spell-checker.handler/set-api-key!
      spell-checker.handler/fn-ret spell-checker.handler/definitions
      spell-checker.handler/word spell-checker.handler/wrap-request
      spell-checker.handler/synonyms spell-checker.handler/frequency
      spell-checker.handler/request}

Of course you might not want every function in that namespace being checked, as some don't need to be, so before handing it over we could take out things like `json->clj`, `defaults` and focus on the core issues.

`stest/check` does produce quite a lot of noise, especially if we are passing it a big set of symbols like `enumerate-namespace` wants to do. Add that with the fact I'm making HTTP requests, giving me a notification for each one,  and  the stacktraces that come along with any errors. A nice thing to use is `stest/abbrev-result` to return a cleaner version of whatever `check` returns, so you can easily see which property (or multiple) ended up failing.

Now all this isn't for production code. The role of these functions is to quickly and dynamically setup a testing environment for our specs , so they can be used in tandem with other things like generators, which I will get into more as we go on.

And this sums up this part of the spell-checker program. I'm going to move onto a different aspect, which is working more with the dictionary.

Let's say our dictionary gets an upgrade, where instead of a just a set of words, each word has a subsequent description of it's popularity and it's definitions. We could use a vector to comprise these three attributes.  We could get by with `s/coll-of` for this sort of thing as none of the attributes will change between a given word, but using the `s/tuple` macro instead is much nicer. Essentially `s/tuple` will conform each value of the vector with the predicate that is at the index. So if you gave it two predicates it would expect a vector of length 2, applying the first spec at index 0 etc.
    
    ;; so to describe our new dictionary, we will check index 0 for a word, 
    ;; index 1 for the popularity and index 2 for the definitions.
    (s/def ::each-word (s/tuple ::word ::perMillion ::definitions))
    
    ;; given an example element of our dictionary...
    (s/valid? ::each-word ["sorting" 6.78 [{:definition "..." :partOfSpeech "verb"}]])
    ;; => true
    
    (s/conform ::each-word ["sorting" 6.78 [{:definition "..." :partOfSpeech "verb"}]])
    ;; => ["sorting" 6.78 [{:definition "..." :partOfSpeech "verb"}]]

Then we can use `s/coll-of` as the wrapper to `s/tuple` to check every vector in our set. Now another way of doing this would be to use something like `s/cat` which essentially combines the "tagging" functionality of `s/or` with `s/tuple` to provide more a reference for each element.

    (doc s/cat)
    
    -------------------------
    clojure.spec.alpha/cat
    ([& key-pred-forms])
    Macro
      Takes key+pred pairs, e.g.

      (s/cat :e even? :o odd?)

      Returns a regex op that matches (all) values in sequence, returning a map
      containing the keys of each pred and the corresponding value.
    nil
    
    ;; essentially like s/tuple, but we can tag pairs with keys for a better description of the data, 
    ;; and it will conform to a map.
    
If we wanted a more descriptive specification, we would use this spec to include some tags, and it does the same sequential, index-style checking that we saw above.

    (s/def ::each-word (s/cat :the-word ::word :popularity-per-million ::perMillion :definitions ::definitions))
    
    (s/conform ::each-word ["siren" 9.56 [{:definition "loud noise" :partOfSpeech "noun"}]])
    {:the-word "siren",
     :popularity-per-million 9.56,
     :definitions [{:definition "loud noise", :partOfSpeech "noun"}]}
    
    ;; in this simple example, data is remoulded into a map but specs in `s/cat` may do further modifications
    
    (s/conform ::each-word ["siren" 9.56 [{:definition "loud noise" :partOfSpeech "noun"}]
                            "hello" 5.23 [{:definition "standard greeting" :partOfSpeech "exclamation"}]])
    
    ;; => :clojure.spec.alpha/invalid 
    
Keep in mind that by defining three keys we expect a collection of three elements,  any more will throw an error because of "added input". Even if we do multiples of three it wouldn't work as `s/cat` expected three elements.  Luckily for us though we don't need to worry about this edge case as every vector has a count of three, meaning we can do `(s/coll-of ::each-word)` on the dictionary without any worries. 

But what *would* we have to do if we were to have our data structured like the second example? Well, up to now we have only used one of the regex functions that come with spec , namely `s/cat`. But there are other functions that can help us when working with sequences:
    
    s/* --> takes a predicate and matches against a sequence where 0 or more values 
            fit the predicate. Conform to vector
    s/+ --> takes a predicate and matches against a sequence where 1 or more values 
            fit the predicate. Conform to vector.
    s/? --> takes a predicate and matches against zero or no values, 
            will return value if it fits predicate.

These are some of
the functions that will help us to manipulate sequences like lists or vectors. Maps have things like `s/map-of`, `s/keys` to describe them, and in combination with those tools we can describe data in any combination of data structure.

Back to our dictionary, we expect 1 or more items, with the regular expression being "1 string , 1 number , 1 string". So we can use the `s/+` macro to achieve this.

    (s/def ::an-entry (s/cat :the-word ::word :popularity-per-million ::perMillion :definitions ::definitions))
    (s/def ::all-entries (s/+ ::an-entry))

Now then, let's try some example values:

    (def mini-dictionary  ["siren" 9.56 [{:definition "loud noise" :partOfSpeech "noun"}]
                           "hello" 5.23 [{:definition "standard greeting" :partOfSpeech "exclamation"}]])

    (s/conform ::all-entries mini-dictionary)
    [{:the-word "siren",
      :popularity-per-million 9.56,
      :definitions [{:definition "loud noise", :partOfSpeech "noun"}]}
     {:the-word "hello",
      :popularity-per-million 5.23,
      :definitions
      [{:definition "standard greeting", :partOfSpeech "exclamation"}]}]

Funnily enough the first time I did this spec it failed, as the word "hello" has the "exclamation" value with the `:partOfSpeech` key, which is perfectly valid but wasn't in the spec. And I guess that was a mini-reminder as why spec is useful as it can bring up properties of the data that you forget about. 

With `s/cat` we define the regular expression to be 3 items, and then `s/+` takes that regular expression and matches it to 3n groups of elements in the sequence. There must be at least one item for the spec to run, and then there must be equal groups of three. If there isn't the spec will fail. If we didn't mind the incoming dictionary being empty, then we could have used `s/*` instead, which just returns an empty vector if the dictionary is empty.

    (s/def ::all-entries (s/* ::an-entry))
    
    (s/conform ::all-entries [])
    ;; => []

Now this could be helpful to other functions that may use this in their pipeline, but it depends on the situation. I think in this case it would be better if we failed as other specs like `::word` need the dictionary to validate things. We don't want `::word` constantly giving `false` if the word does exist, so let's crash and find out what went wrong.

Keep in mind using these two functions is only good for these single-layered collections, if each element were within it's own data structure then we would have to use `s/coll-of` , `s/map-of` , or , as we shall we in a minute , `s/spec`.

The `s/?` function is a bit different from the other two, as it just takes a sequence of zero or one values and checks it against the spec. Now whilst not massively useful on it's own, it is actually quite handy for one particular case. For some words in the dictionary, they could be trademarked which means they could be upper or lower case. But if we allowed some words to be up
percase then our checks in `::word` would cause those valid words to fail. What we can do is add the optional key/value pair `:trademark true/false` , which would indicate that this word is special. 

For some words in our dictionary, they could be trademarked which means they could be either upper or lower case. In the `words.txt.` file that I'm using, all the words are lower-case but it could be useful to notify the end user that the word they searched for is trademarked. This would also be a handy check for `::word` as another one of the specs included could be `::trademark` which checks if the value is present. Combine this with `s/cat` and we've got our functionality:

    (s/def ::trademark (s/? boolean?))
    
    
    ;; just need to tweak the ::an-e
    ntry spec
    (s/def ::an-entry (s/cat :the-word ::word 
                             :popularity-per-million ::perMillion 
                             :definitions ::definitions
                             :trademark ::trademark))
    
    (s/def ::all-entries (s/coll-of ::an-entry))

    ;; with trademark
    (s/conform ::all-entries ["Claxon" 3.89 [{:definition "type of air horn" 
                               :partOfSpeech "noun"}] true])
    [{:the-word "claxon",
      :popularity-per-million 3.89,
      :definitions [{:definition "type of air horn", :partOfSpeech "noun"}],
      :trademark true}]
     
    ;; without trade
    [{:the-word "claxon",
      :popularity-per-million 3.89,
      :definitions [{:definition "type of air horn", :partOfSpeech "noun"}]}]
 
And lastly, we can use the `s/&` function, which takes one of these regex operators and then applies any number of additional checks onto the collection the inital spec returns. So in the case of our dictionary, I want to make sure that every entry has a maximum of 4 elements:

    (s/def ::an-entry (s/& (s/cat :the-word ::word 
                             :popularity-per-million ::perMillion 
                             :definitions ::definitions
                             :trademark ::trademark)
                            #(<= (count %) 4))) 

Alright, now that aside let's move onto some other types of nesting. Say we
have a collection like this one:

    ;; word origins for the word mother
    (def word-origin [["Old english" "mōdor"] ["Dutch" "moeder"] ["German" "Mutter"]])
    
    (s/def :spell-checker.core/origins (s/coll-of (s/+ string?)))
    
    (s/valid? :spell-checker.core/origins word-origin)
    ;; false
    
But this won't come as a surprise, because the last item is not a string. But if we were to simply "re-apply" the spec to that item, `(s/coll-of string?)` it would work, as that item is perfectly described by the entire spec. So we need something that , in the case of failure on the specific check, could pass an item that passes the entire check. What we can do is wrap the the code snippet above with `s/spec`. Which, if we look at the docs:

    (s/def :spell-checker.core/origins (s/coll-of (s/spec (s/+ string))))
    
    (s/valid? :spell-checker.core/origins word-origin)
    ;; true
    
But what about if we had a collection which was a bit more arbitrarily nested?

    (def arbitrary-nesting ["it" ["just" ["keeps" ["going" "but" "then" "it" "stops"]]]])

    ;; recall the spec and it will call this one and supply it with the remaining collection
    (s/def :spell-checker.core/check-nesting (s/coll-of (s/or :string string? :recur ::check-nesting)))
    
    (s/valid? :spell-checker.core/check-nesting arbitrary-nesting)
    ; => true

To round off this long tutorial on spec, we will see it's testing capabilities, and learn to wield the power of generators, `test.check` and other goodies. We already started becoming somewhat familar with testing when I introduced function testing for development earlier on, with `s/fdef` and `s/exercise-fn` but that was only scratching the surface! Let's begin. 

Every spec, when it is registered, like this simple spec:

    (s/def ::check-map (s/keys :req-un [::word ::synonyms]))

will call it's underlying implementation when called, so as we are using `s/keys` , it will call something called `map-spec-impl` , which is essentially just how this spec adheres to the Spec protocol. `s/tuple` for example has `tuple-impl`. To adhere to the Spec protocol, you must also provide an implementation for the gen* function. Moreover, all specs have  are wired to be given the capability, so that we can do generative testing. Using the specs that we've defined, let's generate some examples. 

To return the generator of this spec, we use `s/gen`, and to get some values out of this generator , we turn to the `clojure.spec.gen.alpha` namespace, specifically the `generate` function. 

    (require  '[clojure.spec.alpha :as s]
              '[clojure.spec.gen.alpha :as gen]
              '[spell-checker.handler-spec :as hs])

    ;; to generate a single value
    (gen/generate (s/gen ::hs/word))
    ;; => "unconstrainedly" in my case
    
    ;; to generate a sample
    (gen/sample (s/gen ::hs/word))
    ;; => ("undiscontinued"
           "outcompete"
           "cofound"
           "maladjusted"
           "postsign"
           "pycnogonida"
           "multibreak"
           "unflickering"
           "hazinesses"
           "legislative")

One underlying aspect which is fairly important is that `::hs/word` uses a set to generate it's values, so there is no indirection or blurring on how the spec will generate such a value. But if we had chosen to generate words using filters, like "must be lowercase" , "must not include numbers" etc, spec would just randomly generate values *then* apply these checks, so we could end up applying these checks to keywords, characters etc.  That's what so great about sets in spec. If we were to generate words the latter way, we would need to specify , as the first check, that everything should be a string before evaluating all the unique criterium. 

Now then, how about if we generate values using conformer specs, like `s/or` , `s/cat` , `s/alt` ?

     (s/def ::number-or-word (s/or :word ::hs/word :number number?))
     
     (gen/generate (s/gen ::number-or-word))
     "chrestomathic"
     
     (gen/sample (s/gen ::number-or-word))
     ;; ("confrontation" -1 2.0 0.0 0 3.0 "aeroview" "katharina" 0 "theanthropos")

     ;; how about cat?
     (s/def ::number-or-word (s/cat :word ::hs/word :number number?))
     
     (gen/sample (s/gen ::number-or-word))
     ; => (("dulciloquy" -1.0)
          ("holyday" 0.5)
          ("frontierless" -2.0)
          ("vega" 3)
          ("diffinity" 0)
          ("tallowlike" -1)
          ("chirologies" 0)
          ("atlantomastoid" 2.4375)
          ("subfuscous" 50)
          ("dragginess" -2.5))
          
As we can see, we don't actually get back the entire , conformed , result , in what would have been in a map or tuple, we're just handed the raw generated value itself. If we wanted to see the actual generated value, and what the subsequent return value would look lik


e, what we can use is the `exercise` function, geared more toward r
aw data than the `exercise-fn` we made use of earlier: 

    (s/exercise ::number-or-word)

    ;; using "or"
    ;; => ([0.5 [:number 0.5]]
           ["chancelry" [:word "chancelry"]]
           [-1 [:number -1]]
           [-0.75 [:number -0.75]]
           ["gaddish" [:word "gaddish"]]
           ["psychorhythm" [:word "psychorhythm"]]
           [2.53125 [:number 2.53125]]
           ["gauntleting" [:word "gauntleting"]]
           ["inconcussible" [:word "inconcussible"]]
           [0 [:number 0]])

    ;; using "cat"
    ;; => ([("microfungal" -0.5) {:word "microfungal", :number -0.5}]
           [("anal" -0.5) {:word "anal", :number -0.5}]
           [("unharmonising" 1) {:word "unharmonising", :number 1}]
           [("pygmalion" 0) {:word "pygmalion", :number 0}]
           [("widowing" 0) {:word "widowing", :number 0}]
           [("sexualized" 3.5) {:word "sexualized", :number 3.5}]
           [("campana" 7) {:word "campana", :number 7}]
           [("ungelatinous" 1.0) {:word "ungelatinous", :number 1.0}]
           [("wei" -2) {:word "wei", :number -2}]
           [("rebloomed" 0.75) {:word "rebloomed", :number 0.75}]) 

Now let's try generating some more advanced ones:

    (gen/generate (s/gen ::hs/check-frequency))
    
    ;; => {:word "disculpatory",
           :frequency
           {:zipf -0.025056508740817662,
            :perMillion 47171,
            :diversity 0.6838045120239258}}
            
    (gen/generate (s/gen ::hs/check-word))
    ;; too long to put here...

So far we've looked at how we can use these generators , given a spec,  to work on generating some values for us. But what if we could meddle with the generation process itself? Fortunately spec comes with a couple ways of doing this, so as to help us create , when we need it , simpler , more streamlined processes that could return higher quality tests. What I plan to do is to create a generator, that takes each element of our word set , our dictionary , and puts it into the format we mentioned earlier of `[:word ::word :popularity ::perMillion :definitions ::definitions]`. Now to actually do this, with accuracy,  I would have to make two requests to the WordsAPI per word, and being on only the free tier myself I could only do 1250 words before hitting my limit. But as we're doing this solely with spec for the moment, there is no such limit we need to worry about.

The first tool we have in our box is the `s/with-gen` function:

    (doc s/with-gen)
    
    -------------------------
    clojure.spec.alpha/with-gen
    ([spec gen-fn])
      Takes a spec and a no-arg, generator-returning fn and returns a version of that spec that uses that generator
    nil

Essentially, we're taking a normal spec and attaching a tailored generator, filled with unique checks and such.

    ;; take this simple spec
    (s/def ::new-word string?)
    
    ;; what are the chances that it's going to generate a word? 
    ;; without using our dictionary set, this is how we could help
    ;; this spec become a bit more useful
    
    (defn just-letters? [word] (every? #(Character/isLetter %) word))
    (defn lower-case? [word] (every? #(Character/isLowerCase %) word))

    (s/def ::new-word (s/with-gen string? 
                          #(s/gen (s/and string? just-letters? lower-case?))))

Whilst you can try all this out and get some stings that resemble something of words, it will be a long time before you get something actually in the dictionary. In fact, I tried this many times and got either "" or just a one letter word. What we can do is introduce a wrapper around our generator, using `gen/such-that`  to apply constraints. This is preffered over introducing more checks in the spec itself, which increases it's complexity , it's "noise" and it allows the spec itself to be cleaner and more reusable.

     (s/def ::new-word (s/with-gen string? 
                            #(gen/such-that (complement empty?) 
                                (s/gen  (s/and string? just-letters? lower-case?)))))

But then we enter another problem which is that we are more likely to get the `couldn't satisfy predicate after 100 tries` error. This is ultimately why we use the dictionary, as the passing values are too niche and specific, and are not a "range" so to speak.

Moving on, toward our goal of a re-vamped dictionary, the aim is to have our original dictionary be the set, of which another function will take a value , alongside other specs and `s/cat` them together. We can use `gen/tuple` to setup the things we need for `s/cat`. `gen/tuple` takes a series of generator functions and then just pops them into a vector, which we can just pass along:

     (s/def ::new-word (s/with-gen (s/cat :word ::word :popularity ::perMillion :definitions ::definitions)
                          #(gen/tuple (s/gen ::word) (s/gen ::perMillion) (s/gen ::definitions))))
                          
     ;; but if we do
     (gen/generate (s/gen ::new-word))
     ;; it will just return the vector of ["word" popularity definitions] 
     ;; and so I would have to use conform to get the map that s/cat returns
     
     ;; one way of doing it would be to use the same spec, 
     ;; just the generator for the value bit and conform using the spec.
     (s/conform ::new-word (gen/generate (s/gen ::new-word)))
 
By doing a bit of snooping I came across a function that's actually much nicer and simplifies this problem a lot, I don't really need a separate spec *and* generator, for testing all I need is a generator, which comes in the form of `gen/hash-map` , which takes kv pairs, and every value should be a generator:

    (gen/generate (gen/hash-map  :word (s/gen ::word) :popularity (s/gen ::perMillion) 
                                 :definitions (s/gen ::definitions)))

You can generate most types, otherwise use `s/gen`, most data structures, and all sorts of nice helper functions. So far we've just been in the spec world, and only used spec functions, spec generators. But there is one function that helps us to bridge that gap, and allow us to mix in our own clojure functions right into the generation process. This is done with a handy little function called `gen/fmap`. 

Fmap leverages the functionalities of a basic map, and will take a function , not a spec , not necessarily a predicate either , and apply it  to the generator. Since `gen/fmap` itself returns a generator , we don't need to use an `s/gen` wrapper, otherwise we will get an assertion error as all the spec would contain is the generator and no checks. Moreover, if we don't need to use `s/gen` don't define functions like the one below as specs, as you'll get NPEs when trying to generate values from them. Although if you are using `gen/fmap` as a component of a larger function, like if we were to incorporate with something like `s/with-gen` , then that is indicative that we would go about defining it as a spec. 

    ;; helper functions to help fmap generate accurate results
    (defn grab-popularity
      [word]
      (-> (frequency word)
          (:frequency)
          (:perMillion)))
    
    (defn grab-definitions
      [word]
      (-> (definitions word)
          (:definitions)))

    ;; so this could be how we would generate accurate words, using the API functions themselves,
    ;; but you might want to swap them out for generated results while testing.
    ;; we can keep the (s/gen ::word) set as the set is the same as our dictionary, and we need
    ;; a generator for it to work.
    (def new-word (gen/fmap (fn [word] [word (grab-popularity word) (grab-definitions word)])
      (s/gen ::word)))

I guess the last step in our generatory journey would be to see if it can handle our `multi-spec` we defined much much earlier on. We would hope to see maps of different keys, so of differing types , and in the case of the `:word` method, we hope to see many different lengths of response (as some of the keys are optional). Firstly though, to illustrate all the technicalities and intricacies of spec, I'll show you a simpler multi-spec example and we'll study the different sorts of things we generate and the process of doing so.

    ;; these are just keys to differentiate between methods
    (s/def ::example-key keyword?)
    (s/def ::different-key keyword?)
    
    ;; spec which checks ::tag key
    (s/def ::tag keyword?)
    
    ;; grabbing value out of :tag , which is in all maps     
    (defmulti example :tag)
    
    (defmethod example :a [_]
      (s/keys :req-un [::tag ::different-key]))
    
    (defmethod example :b [_] 
      (s/keys :req-un [::tag ::example-key]))

    ;; not nil as we're going to be generating.
    (s/def ::example (s/multi-spec example *not nil! but what is it ?!*))
    
Well, the second argument of `s/multi-spec` is called retag and it can be either a keyword or a function. In the case of a keyword, when the spec is created,  `s/multi-spec` will generate a function for us to use , and this is the default fn used.

    ;; taken from the clojure.spec.alpha source code, the impl for multi-spec
    ...
    ;; retag is either the dispatch tag , which is usually a keyword , or a function
    ;; which would then be the generator function
    ;; based on that value will be the method we call.
    (if (keyword? retag)
                 #(assoc %1 retag %2)
                 retag)

This generator function that spec would give us takes two args. I'll talk about the second argument first as it's more important , and sort of explains where the first is coming from. The second argument is the dispatch tag and one is picked each time we call (gen/generate ...), for the example above this would be `:a` or `:b`. Now we've got that tag, it then grabs the method with that key , so if the dispatch tag was `:a` then it would grab the spec out of method `:a` and generate some values using the spec from the method body. In this case it would use `s/keys` and make a map of `:tag` and `:different-key`. This generated map is what is passed as the first argument. During generation, our function sits atop the generation process , accepting a map and the corresponding tag,  and filter out (or reformat) certain attributes. But most of the time we just want the spec as it is. This `assoc`ing simply overrides the random value generated for the correct, corresponding dispatch tag. This is why it is called `retag` , as we are retagging the randomly generated keyword for the actual, correct, tag , being whatever method attribute was picked.
    ... 
    ;; same code as above just with 
    (s/def ::example (s/multi-spec example :tag))
    
    ;; so we're going to using the default function, but doing
    ;; (s/def ::example (s/multi-spec example (fn [genv tag] (assoc genv :tag tag))))
    ;; is equivalent.

And when we generate a value:

    (gen/generate (s/gen ::example))
    ;; => {:tag :b, :example-key :OOpmzE+?qRU?9fjYRg?PYEZ*i7/q_23g200}
    
For the eagle-eyed reader that managed to digest the last paragraph, I mentioned that the point of `assoc`ing was to have the `:tag` match the correct method attribute. So what if we didn't assoc to `:tag` but to something like `:tagg`? Well, depending on if our spec allows this is irrelevant as `:tag` will fail if the value isn't overriden.

    (s/def ::example (s/multi-spec example (fn [genv tag] (assoc genv :tagg tag))))
    (gen/generate (s/gen ::example))
    ;; => couldn't satisfy predicate after 100 tries...
    
But we see it isn't a problem with `s/keys` as it will let other keys through, if I `assoc`ed tag properly and stuck something else onto `:tagg` we pass.

    (s/def ::example (s/multi-spec example (fn [genv tag] (assoc (assoc genv :tag tag) :tagg "some val"))))
    (gen/generate (s/gen ::example))
    
    ;; => {:tag :b, :example-key :-*--+!P-b?E?J81*R2, :tagg "some val"}
    
What about when I decide to override the tag key for a different method attribute? So if I just wanted `:a` to be tested, could that work?

    (s/def ::example (s/multi-spec example (fn [genv tag] (assoc (assoc genv :tag tag) :tag :a))))
    (gen/generate (s/gen ::example))
    
    ;; it does, it will just keep trying until it generates a set for a particular method
    ;; that has the same keyword as the one we assoced.
    ;; => {:tag :a, :different-key :bF.k1Es1?q0kk1Y_d*xDT2z+DzSA-!L*rO.fw**C22sGvZ2_R-JeP1*b.+R2!g!?V*PpWmdwp*._t!c2zM7_Oh3-78PI-7Y_N0*o0.PN33pxy0_y7L4-68*j.I6?.d3**-.C+.jio+S_*W5-_6v?5qq1lGhk.p0j.x621Wl21m*5*T+w09HdYI3_.i-?F_5?W8BHJ.!-2037H-*2UB4_f*V!8XL_/s++Tcc4++D*S2*?Q+Z_?52!84}

Up till now , all we have been doing is working with maps and `multi-spec`. For this use case it is perfectly fine, and the default function is perfectly useful and we have no difficulty producing a range of samples, but what if we aren't working with maps? We may have to do [something different](https://stackoverflow.com/questions/45394546/what-does-retag-parameter-in-s-multi-spec-mean). Take a look at the example, but be careful. Not supplying anything atop the generated value **only works when the spec in the method does the job of matching the exact dispatch key.** Here is the code from that example:

    (defmulti foo first)
    (defmethod foo :so/one [_]
      (s/cat :typ #{:so/one} :num number?))
    (defmethod foo :so/range [_]
      (s/cat :typ #{:so/range} :lo number? :hi number?))
      
`defmulti` will look at the first value, and depending on it being `:so/one` or `:so/two` it will push to either method. `s/cat` then does the work of making sure that the first argument is the same as the key that would be generated. Moreover, there is no point including any modifications atop the method's spec as it would cause issues.

In cases though where we are working with non-maps *and* don't have the spec itself producing perfect generations, it means we must include a generator function. Let's take a look at this multi-spec:

Say the data we're working with is in the form : ` [{:tag string?}{:example string?}] or  [{:tag string?}{:different-key string?}]` we can see that `:tag` will be the universal identifier, and that is what `defmulti` needs to pull out.
    
    (defmulti vectors-and-maps #(:tag (first %)))

Next up we need to define the methods, let's just say that for one `:tag` we would like to see the value "a" and another we would like to see the value "b":

    (defmethod vectors-and-maps "a" [_]
      (s/tuple ::tag-map ::example-map))

    (defmethod vectors-and-maps "b" [_]
      (s/tuple ::tag-map ::different-map))

And now the specs for these...

    (s/def ::tag (s/and string? #(<= (count %) 10)))
    (s/def ::tag-map (s/keys :req-un [::tag]))

    (s/def ::example-key (s/and string? #(<= (count %) 10)))
    (s/def ::example-map (s/keys :req-un [::example-key]))

    (s/def ::different-key (s/and string? #(<= (count %) 10)))
    (s/def ::different-map (s/keys :req-un [::different-key]))

And lastly,

    (s/def ::example (s/multi-spec vectors-and-maps (fn [genv tag] (assoc-in genv [0 :tag] tag))))

Given that we are creating arbitrary strings we will need to retag, and that is the generator function to do it. Although, if you're thinking that you could just replace the `::tag` spec with a set, you would be absolutely right! And you save yourself the hassle of retagging. The set only specifies the accepted method attributes, it won't take long to generate a value from the set that matches the dispatch tag.

    (s/def ::tag #{"a" "b"})
    
    ...
    
    (s/def ::example (s/multi-spec vectors-and-maps (fn [genv tag] genv)))
    (gen/generate (s/gen ::example))
    ;; => [{:tag "b"} {:different-key "W7Ok86"}]

And that about wraps up about everyhting from me. I urge you to play around with all the generators spec, all the generators `gen` has to offer, and by all means use the specs that I've been using in this guide, all of which can be found [here](https://www.github.com/Alex-Bakic/spell-checker). I'm afraid this is the end, but there is still lots to cover! I've missed some things about spec, such as validating macros, working with databases but I'm sure after reading all this you are ready to explore any and all things spec for yourself. 

Before I head off, I do want to briefly speak of the changes that are coming to spec in the near future, in the `alpha2` version of the library. Things like `s/nest` replacing `s/spec` as it is more intuitive, and a whole host of changes are happening under the hood too, I mentioned earlier most specs have an underlying `impl` , well that's changing to be more data oriented. You can see the direction of it by looking at the old implementation for `s/+` and the new implementation for it, which I do think will be having it much more aligned with the rest of clojure (data oriented and programmable). For a good explanation on this , see the [InsideClojure journal entry on this shift](http://insideclojure.org/2019/01/11/journal/). Other tools like `s/select` are predicated to be quite big, a talk of what it does and it's ethos can be given from [this talk](https://www.youtube.com/watch?v=YR5WdGrpoug&feature=youtu.be), by the creator himself. 

Further Reading:

[The spec guide](https://clojure.org/guides/spec)
[Discussion on using fully-qualified namspaces for keys](http://insideclojure.org/2019/03/16/journal/)
[s/select](http://insideclojure.org/2019/03/16/journal/)
[where to put my specs?](https://stackoverflow.com/questions/37942495/where-to-put-specs-for-clojure-spec) 
[Spec as a runtime transformation engine](https://www.metosin.fi/blog/clojure-spec-as-a-runtime-transformation-engine/)
