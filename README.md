# spell-checker

A Clojure wrapper for the Words API

# usage

    ;; need to call the spell-checker.handler/set-api-key! function
    ;; this is so you can make requests when you call the wrapper functions
    ;; you can get a key by signing up for the free tier of Words API
    
    (require '[spell-checker.handler :refer :all])
    
    (set-api-key! "will-need-to-be-a-string")
    
    ;; now you can call any of the public fns
    
    (synonyms "monad") 
    ;; => {:word "monad" :synonyms ["all" "the" "synonyms"]}
    
To get your free tier key, head to [rapid api](https://rapidapi.com/dpventures/api/wordsapi/) where you can make a few demo requests. Log in through github , google or facebook and you should be taken back to the menu, where you need to click on pricing. Then choose to subscribe for the free tier. 

**Note** : Even subscribing to the free tier requires credit card information, in case you go over your alloted request amount, but for me personally I know I can't go over it, as this was just a hobbie program. The free tier allows you to do 2500 requests per day, which for me is plenty.
  
## License

Copyright © 2019 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
