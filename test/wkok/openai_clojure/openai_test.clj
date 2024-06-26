(ns wkok.openai-clojure.openai-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing]]
   [martian.core :as martian]
   [wkok.openai-clojure.openai :as openai]))

(defn- find-multipart
  [request name]
  (->> request
       :multipart
       (some #(when (= name (:name %)) %))))

(deftest multipart-request-format
  (testing "request has the correct format"
    (let [request (martian/request-for @openai/m :create-image-edit
                                       {:image (io/file "path/to/otter.png")
                                        :mask (io/file "path/to/mask.png")
                                        :prompt "A cute baby sea otter wearing a beret"
                                        :n 2
                                        :size "1024x1024"
                                        :wkok.openai-clojure.core/options {:api-key "123"}})]

      (testing "contains Authorization header"
        (is (contains? (:headers request) "Authorization")))

      (testing "multipart image file set correctly"
        (let [otter (find-multipart request "image")]
          (is (instance? java.io.File (:content otter)))))

      (testing "multipart prompt set correctly"
        (let [prompt (find-multipart request "prompt")]
          (is (= "A cute baby sea otter wearing a beret"
                 (:content prompt)))))

      (testing "options not set in multipart fields"
        (is (nil? (find-multipart request "options")))))))

(deftest add-headers-init
  (let [add-headers-fn (-> openai/add-headers :enter)]
    (testing "atoms get initialized correctly"

      (is (= {"Authorization" "Bearer my-secret-key",
              "OpenAI-Organization" "my-company"
              "OpenAI-Beta" "assistants=v2"}
             (-> (add-headers-fn {:params {:wkok.openai-clojure.core/options {:api-key "my-secret-key"
                                                                              :organization "my-company"
                                                                              :openai-beta "assistants=v2"}}})
                 :request
                 :headers))))))

(deftest override-api-endpoint-test
  (let [base-url "https://api.openai.com/v2"
        override-api-endpoint-fn (-> base-url openai/override-api-endpoint :enter)]
    (testing "api endpoint gets correctly overridden"

      (let [api-endpoint "https://myendpoint.my-openai.com/v2"
            path "/some/chat/prompt"
            test-fn (fn [url]
                      (is (= (str api-endpoint path)
                             (-> (override-api-endpoint-fn {:request {:url url}
                                                            :params {:wkok.openai-clojure.core/options {:api-endpoint api-endpoint}}})
                                 :request
                                 :url))))]
        (test-fn (str base-url "/some/chat/prompt"))))))
