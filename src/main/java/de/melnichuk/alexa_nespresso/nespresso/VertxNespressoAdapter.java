package de.melnichuk.alexa_nespresso.nespresso;

import de.melnichuk.alexa_nespresso.nespresso.model.Item;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class VertxNespressoAdapter implements NespressoAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(VertxNespressoAdapter.class);

    private static final int NESPRESSO_PORT = 443;
    private static final String NESPRESSO_HOST = "www.nespresso.com";

    private final Vertx vertx;

    public VertxNespressoAdapter(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void executeOrder(Collection<Item> items, String username, String password) {
        final Function<WebClient, Future<WebClientContext>> login = constructLoginFunction(username, password);
        final Function<WebClientContext, Future<WebClientContext>> addItems = constructAddItemsFunction(items);
        final Function<WebClientContext, Future<WebClientContext>> checkout = constructCheckoutFunction();
        final Function<WebClientContext, Future<WebClientContext>> logout = constructLogoutFunction();
        final Function<WebClientContext, Future<Void>> cleanupAfterOrder = constructCleanupAfterOrderFunction();
        final WebClient webClient = WebClient.create(this.vertx);

        Future.succeededFuture(webClient)
                .compose(login)
                .compose(addItems)
                .compose(checkout)
                .compose(logout)
                .compose(cleanupAfterOrder);
    }

    private Function<WebClientContext, Future<WebClientContext>> constructAddItemsFunction(Collection<Item> items) {
        return webClientContext -> {
            LOGGER.trace("addItems@nespresso.com");
            return Future.future(future -> {
                Future<WebClientContext> currentAddItemStep = Future.succeededFuture(webClientContext);

                for (Item item : items) {
                    final String productId = item.getProduct().getProductId();
                    final int quantity = item.getQuantity();
                    final Function<WebClientContext, Future<WebClientContext>> addItem = constructAddItemFunction(productId, quantity);
                    currentAddItemStep = currentAddItemStep.compose(addItem);
                }

                currentAddItemStep.setHandler(asyncResult -> {
                    if (asyncResult.succeeded()) {
                        future.complete(webClientContext);
                    } else {
                        future.fail(asyncResult.cause());
                    }
                });
            });
        };
    }

    private Function<WebClientContext, Future<Void>> constructCleanupAfterOrderFunction() {
        return webClientContext -> {
            LOGGER.trace("cleanupAfterOrder@nespresso.com");
            return Future.future(future -> {
                webClientContext.getWebClient().close();
                future.complete();
            });
        };
    }

    // i only managed to fulfil a order by sending multiple requests to /de/de/checkout in a order i observed in the browser.
    // a sane public API would be appreciated.
    private Function<WebClientContext, Future<WebClientContext>> constructCheckoutFunction() {
        return webClientContext -> {
            LOGGER.trace("checkout@nespresso.com");

            return Future.succeededFuture(webClientContext)
                    .compose(constructFirstCheckoutRequestFunction())
                    .compose(constructSecondCheckoutRequestFunction())
                    .compose(constructThirdCheckoutRequestFunction());
        };
    }

    private Function<WebClientContext, Future<WebClientContext>> constructFirstCheckoutRequestFunction() {
        return webClientContext -> {
            LOGGER.trace("first-checkout-request@nespresso.com");
            return Future.future(future -> getWithSSL(webClientContext, "/de/de/checkout").send(asyncResult -> {
                if (asyncResult.succeeded()) {
                    final int statusCode = asyncResult.result().statusCode();
                    LOGGER.trace("first request to /checkout returned with statusCode={}.", statusCode);

                    if (statusCode == 200) {
                        future.complete(webClientContext);
                    } else {
                        final RuntimeException cause = new RuntimeException("first requst to /checkout failed.");
                        future.fail(cause);
                    }
                } else {
                    future.fail(asyncResult.cause());
                }
            }));
        };
    }

    private Function<WebClientContext, Future<WebClientContext>> constructSecondCheckoutRequestFunction() {
        return webClientContext -> {
            LOGGER.trace("second-checkout-request@nespresso.com");
            return Future.future(future -> getWithSSL(webClientContext, "/de/de/checkout").addQueryParam("execution", "e1s1").send(asyncResult -> {
                if (asyncResult.succeeded()) {
                    final HttpResponse<Buffer> response = asyncResult.result();
                    final int statusCode = asyncResult.result().statusCode();
                    LOGGER.trace("second request to /checkout returned with statusCode={}.", statusCode);

                    if (statusCode == 200) {
                        future.complete(webClientContext);
                    } else {
                        final RuntimeException cause = new RuntimeException("second requst to /checkout failed.");
                        future.fail(cause);
                    }
                } else {
                    future.fail(asyncResult.cause());
                }
            }));
        };
    }

    private Function<WebClientContext, Future<WebClientContext>> constructThirdCheckoutRequestFunction() {
        return webClientContext -> {
            LOGGER.trace("third-checkout-request@nespresso.com");
            return Future.future(future -> {
                MultiMap form = constructThirdCheckoutRequestForm();
                postWithSSL(webClientContext, "/de/de/checkout").addQueryParam("execution", "e1s1").sendForm(form, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        final HttpResponse<Buffer> response = asyncResult.result();
                        final int statusCode = asyncResult.result().statusCode();
                        LOGGER.trace("third request to /checkout returned with statusCode={}.", statusCode);

                        if (statusCode == 200 || statusCode == 302) {
                            future.complete(webClientContext);
                        } else {
                            final RuntimeException cause = new RuntimeException("third requst to /checkout failed.");
                            future.fail(cause);
                        }
                    } else {
                        future.fail(asyncResult.cause());
                    }
                });
            });
        };
    }

    private MultiMap constructThirdCheckoutRequestForm() {
        return MultiMap.caseInsensitiveMultiMap()
                .add("_eventId_continue", "")
                .add("_termConfirmed", "on")
                .add("_saveAsDefault", "on")
                .add("_enable2ClicksCheckout", "on")

                // TODO
                //.add("_termConfirmed", "true")

                .add("_saveAsDefault", "true")
                .add("_enable2ClicksCheckout", "true");
    }

    private Function<WebClientContext, Future<WebClientContext>> constructAddItemFunction(String productId, int quantity) {
        return webClientContext -> {
            LOGGER.trace("addItem@nespresso.com");
            return Future.future(future -> {
                MultiMap form = constructAddItemForm(productId, String.valueOf(quantity));
                postWithSSL(webClientContext, "/mosaic/de/de/ecapi/1/cart/update").sendForm(form, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        final HttpResponse<Buffer> response = asyncResult.result();
                        final int statusCode = asyncResult.result().statusCode();
                        if (statusCode == 200 || statusCode == 302) {
                            future.complete(webClientContext);
                        } else {
                            final RuntimeException cause = new RuntimeException("addItem(productId=" + productId + "; quantity=" + quantity + ") http request resulted in statusCode=" + statusCode + ".");
                            future.fail(cause);
                        }

                        final String body = response.bodyAsString();
                        LOGGER.debug("response body for addItem request: {}", body);
                    } else {
                        future.fail(asyncResult.cause());
                    }
                });
            });
        };
    }

    private Function<WebClientContext, Future<WebClientContext>> constructLogoutFunction() {
        return webClientContext -> {
            LOGGER.trace("logout@nespresso.com");
            return Future.future(future -> getWithSSL(webClientContext, "/de/de/logout").send(asyncResult -> {
                if (asyncResult.succeeded()) {
                    final int statusCode = asyncResult.result().statusCode();
                    if (statusCode == 200 || statusCode == 302) {
                        final HttpResponse<Buffer> response = asyncResult.result();
                        future.complete(webClientContext);
                    } else {
                        future.fail(asyncResult.cause());
                    }
                } else {
                    future.fail(asyncResult.cause());
                }
            }));
        };
    }

    private Function<WebClient, Future<WebClientContext>> constructLoginFunction(final String username, final String password) {
        return webClient -> {
            LOGGER.trace("login@nespresso.com");
            return Future.future(future -> {
                final MultiMap form = constructLoginForm(username, password);
                postWithSSL(webClient, "/mosaic/de/de/ecapi/1/authentication/login").sendForm(form, asyncResult -> {
                    if (asyncResult.succeeded()) {
                        final int statusCode = asyncResult.result().statusCode();
                        LOGGER.debug("login request returned with statusCode={}.", statusCode);

                        if (statusCode == 200 || statusCode == 302) {
                            final HttpResponse<Buffer> response = asyncResult.result();
                            LOGGER.debug("cookies for login request: {}", response.cookies());

                            final WebClientContext webClientContext = new WebClientContext(webClient, response.cookies());
                            future.complete(webClientContext);
                        } else {
                            future.fail(asyncResult.cause());
                        }
                    } else {
                        future.fail(asyncResult.cause());
                    }
                });

            });
        };
    }


    private HttpRequest<Buffer> postWithSSL(WebClientContext webClientContext, String uri) {
        HttpRequest<Buffer> request = postWithSSL(webClientContext.getWebClient(), uri);
        webClientContext.getCookies().forEach(cookie -> request.headers().add(HttpHeaders.COOKIE, cookie));
        return request;
    }

    private HttpRequest<Buffer> postWithSSL(WebClient webClient, String uri) {
        return webClient.post(NESPRESSO_PORT, NESPRESSO_HOST, uri).ssl(true);
    }

    private HttpRequest<Buffer> getWithSSL(WebClientContext webClientContext, String uri) {
        HttpRequest<Buffer> request = getWithSSL(webClientContext.getWebClient(), uri);
        webClientContext.getCookies().forEach(cookie -> request.headers().add(HttpHeaders.COOKIE, cookie));
        return request;
    }

    private HttpRequest<Buffer> getWithSSL(WebClient webClient, String uri) {
        return webClient.get(NESPRESSO_PORT, NESPRESSO_HOST, uri).ssl(true);
    }

    private MultiMap constructAddItemForm(String productId, String quantity) {
        return MultiMap.caseInsensitiveMultiMap()
                .add("productId", productId)
                .add("quantity", quantity);
    }

    private MultiMap constructLoginForm(String username, String password) {
        return MultiMap.caseInsensitiveMultiMap()
                .add("_spring_security_remember_me", "false")
                .add("j_username", username)
                .add("j_password", password);
    }

    private class WebClientContext {
        private final WebClient webClient;
        private final List<String> cookies;

        public WebClientContext(WebClient webClient, List<String> cookies) {
            this.webClient = webClient;
            this.cookies = cookies;
        }

        public WebClient getWebClient() {
            return webClient;
        }

        public List<String> getCookies() {
            return cookies;
        }
    }
}
