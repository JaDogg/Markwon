package io.noties.markwon.sample.basicplugins;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.commonmark.node.Heading;
import org.commonmark.node.Paragraph;

import java.util.Collection;
import java.util.Collections;

import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.Markwon;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonPlugin;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.MarkwonVisitor;
import io.noties.markwon.RenderProps;
import io.noties.markwon.core.MarkwonTheme;
import io.noties.markwon.html.HtmlPlugin;
import io.noties.markwon.html.HtmlTag;
import io.noties.markwon.html.tag.SimpleTagHandler;
import io.noties.markwon.image.ImageItem;
import io.noties.markwon.image.ImagesPlugin;
import io.noties.markwon.image.SchemeHandler;
import io.noties.markwon.image.network.NetworkSchemeHandler;
import io.noties.markwon.movement.MovementMethodPlugin;

public class BasicPluginsActivity extends Activity {

    private TextView textView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        textView = new TextView(this);
        setContentView(textView);

        step_1();

        step_2();

        step_3();

        step_4();

        step_5();

        step_6();
    }

    /**
     * In order to apply paragraph spans a custom plugin should be created (CorePlugin will take care
     * of everything else).
     * <p>
     * Please note that when a plugin is registered and it <em>depends</em> on CorePlugin, there is no
     * need to explicitly specify it. By default all plugins that extend AbstractMarkwonPlugin do declare
     * it\'s dependency on CorePlugin ({@link MarkwonPlugin#priority()}).
     * <p>
     * Order in which plugins are specified to the builder is of little importance as long as each
     * plugin clearly states what dependencies it has
     */
    private void step_1() {

        final String markdown = "# Hello!\n\nA paragraph?\n\nIt should be!";

        final Markwon markwon = Markwon.builder(this)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
                        builder.setFactory(Paragraph.class, (configuration, props) ->
                                new ForegroundColorSpan(Color.GREEN));
                    }
                })
                .build();

        markwon.setMarkdown(textView, markdown);
    }

    /**
     * To disable some nodes from rendering another custom plugin can be used
     */
    private void step_2() {

        final String markdown = "# Heading 1\n\n## Heading 2\n\n**other** content [here](#)";

        final Markwon markwon = Markwon.builder(this)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureVisitor(@NonNull MarkwonVisitor.Builder builder) {

                        // for example to disable rendering of heading:
                        // try commenting this out to see that otherwise headings will be rendered
                        builder.on(Heading.class, null);

                        // same method can be used to override existing visitor by specifying
                        // a new NodeVisitor instance
                    }
                })
                .build();

        markwon.setMarkdown(textView, markdown);
    }

    /**
     * To customize core theme plugin can be used again
     */
    private void step_3() {

        final String markdown = "`A code` that is rendered differently\n\n```\nHello!\n```";

        final Markwon markwon = Markwon.builder(this)
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureTheme(@NonNull MarkwonTheme.Builder builder) {
                        builder
                                .codeBackgroundColor(Color.BLACK)
                                .codeTextColor(Color.RED);
                    }
                })
                .build();

        markwon.setMarkdown(textView, markdown);
    }

    /**
     * MarkwonConfiguration contains these <em>utilities</em>:
     * <ul>
     * <li>SyntaxHighlight</li>
     * <li>LinkSpan.Resolver</li>
     * <li>UrlProcessor</li>
     * <li>ImageSizeResolver</li>
     * </ul>
     * <p>
     * In order to customize them a custom plugin should be used
     */
    private void step_4() {

        final String markdown = "[a link without scheme](github.com)";

        final Markwon markwon = Markwon.builder(this)
                // please note that Markwon does not handle MovementMethod,
                // so if your markdown has links your should apply MovementMethod manually
                // or use MovementMethodPlugin (which uses system LinkMovementMethod by default)
                .usePlugin(MovementMethodPlugin.create())
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configureConfiguration(@NonNull MarkwonConfiguration.Builder builder) {
                        // for example if specified destination has no scheme info, we will
                        // _assume_ that it's network request and append HTTPS scheme
                        builder.urlProcessor(destination -> {
                            final Uri uri = Uri.parse(destination);
                            if (TextUtils.isEmpty(uri.getScheme())) {
                                return "https://" + destination;
                            }
                            return destination;
                        });
                    }
                })
                .build();

        markwon.setMarkdown(textView, markdown);
    }

    /**
     * Images configuration. Can be used with (or without) ImagesPlugin, which does some basic
     * images handling (parsing markdown containing images, obtain an image from network
     * file system or assets). Please note that
     */
    private void step_5() {

        final String markdown = "![image](myownscheme://en.wikipedia.org/static/images/project-logos/enwiki-2x.png)";

        final Markwon markwon = Markwon.builder(this)
                .usePlugin(ImagesPlugin.create())
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configure(@NonNull Registry registry) {

                        // use registry.require to obtain a plugin, does also
                        // a runtime validation if this plugin is registered
                        registry.require(ImagesPlugin.class, plugin -> plugin.addSchemeHandler(new SchemeHandler() {

                            // it's a sample only, most likely you won't need to
                            // use existing scheme-handler, this for demonstration purposes only
                            final NetworkSchemeHandler handler = NetworkSchemeHandler.create();

                            @NonNull
                            @Override
                            public ImageItem handle(@NonNull String raw, @NonNull Uri uri) {
                                final String url = raw.replace("myownscheme", "https");
                                return handler.handle(url, Uri.parse(url));
                            }

                            @NonNull
                            @Override
                            public Collection<String> supportedSchemes() {
                                return Collections.singleton("myownscheme");
                            }
                        }));
                    }
                })
                // or we can init plugin with this factory method
//                .usePlugin(ImagesPlugin.create(plugin -> {
//                    plugin.addSchemeHandler(/**/)
//                }))
                .build();

        markwon.setMarkdown(textView, markdown);
    }

    public void step_6() {

        final Markwon markwon = Markwon.builder(this)
                .usePlugin(HtmlPlugin.create())
                .usePlugin(new AbstractMarkwonPlugin() {
                    @Override
                    public void configure(@NonNull Registry registry) {
                        registry.require(HtmlPlugin.class, plugin -> plugin.addHandler(new SimpleTagHandler() {
                            @Override
                            public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps renderProps, @NonNull HtmlTag tag) {
                                return new AlignmentSpan.Standard(Layout.Alignment.ALIGN_CENTER);
                            }

                            @NonNull
                            @Override
                            public Collection<String> supportedTags() {
                                return Collections.singleton("center");
                            }
                        }));
                    }
                })
                .build();
    }

    // text lifecycle (after/before)
    // rendering lifecycle (before/after)
    // renderProps
    // process
    // priority
}
