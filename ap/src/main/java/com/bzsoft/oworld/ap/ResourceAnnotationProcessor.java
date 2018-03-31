package com.bzsoft.oworld.ap;

import java.awt.Color;
import java.awt.Font;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.bzsoft.oworld.ap.util.AppInfo;
import com.bzsoft.oworld.ap.util.ResourceUtil;

@SupportedAnnotationTypes(value = "com.bzsoft.oworld.ap.ResourceProcessable")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public final class ResourceAnnotationProcessor extends AbstractProcessor {

	public ResourceAnnotationProcessor() {
		//
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		ResourceProcessable a = null;
		for (final TypeElement annotation : annotations) {
			final Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(annotation);
			for (final Element item : annotatedElements) {
				a = item.getAnnotation(ResourceProcessable.class);
				if (a != null) {
					break;
				}
			}
		}
		final Messager msg = processingEnv.getMessager();
		if (a != null) {
			final String appUrl = a.appLocation();
			try {
				final AppInfo appInfo = ResourceUtil.loadApp(appUrl);
				final String colorUrl = appInfo.getColorUrl();
				final String fontUrl = appInfo.getFontUrl();
				final String className = a.className();
				final List<String> resourceUrls = appInfo.getResourceUrls();
				final String charUrl = appInfo.getCharsUrl();
				msg.printMessage(Diagnostic.Kind.NOTE,
						ResourceProcessable.class.getSimpleName() + " annotation found.");
				final Object[][] colors = ResourceUtil.parseProperties(colorUrl);
				final Object[][] fonts = ResourceUtil.parseProperties(fontUrl);
				final Object[][] resources = ResourceUtil.parseProperties(resourceUrls);
				final Object[][] chars = ResourceUtil.parseProperties(charUrl);
				writeNamingFile(className, processingEnv, colors, fonts, null, resources, chars);
			} catch (final Exception e) {
				e.printStackTrace();
				msg.printMessage(Diagnostic.Kind.ERROR,
						ResourceProcessable.class.getSimpleName() + " generation error");
			}
		} else {
			msg.printMessage(Diagnostic.Kind.NOTE,
					ResourceProcessable.class.getSimpleName() + " annotation not found.");
		}
		return true;
	}

	private static final Color parseColor(String c) throws Exception {
		return new Color(Integer.valueOf(c.substring(1, 3), 16), Integer.valueOf(c.substring(3, 5), 16),
				Integer.valueOf(c.substring(5, 7), 16));
	}

	private static final Font parseFont(String str) throws Exception {
		final String[] ss = str.split(",");
		final String name = ss[0].trim();
		final int style = Integer.parseInt(ss[1]);
		final int size = Integer.parseInt(ss[2]);
		return new Font(name, style, size);
	}

	private static void writeNamingFile(String className, ProcessingEnvironment processingEnv, Object[][] colors,
			final Object[][] fonts, String i18nUrl, final Object[][] resources, final Object[][] chars)
			throws Exception {
		String simpleClassName;
		String packageName = null;
		{
			final int lastDot = className.lastIndexOf('.');
			if (lastDot > 0) {
				packageName = className.substring(0, lastDot);
			}
			simpleClassName = className.substring(lastDot + 1);
		}

		final JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(className);
		try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
			if (packageName != null) {
				out.print("package ");
				out.print(packageName);
				out.println(";");
				out.println();
			}

			out.print("public final class ");
			out.print(simpleClassName);
			out.println(" {");
			out.println();
			out.println("public static final class Colors {");
			if (colors != null) {
				int i = 0;
				for (final Object[] cs : colors) {
					final String name = (String) cs[0];
					out.print(" public static final int ");
					out.print(name);
					out.print('=');
					out.print(i);
					out.println(";");
					i++;
				}
				out.println("private static final java.awt.Color[] colors = new java.awt.Color[]{");
				i = 0;
				for (final Object[] cs : colors) {
					final String colorStr = (String) cs[1];
					final Color col = parseColor(colorStr);
					final int r = col.getRed();
					final int g = col.getGreen();
					final int b = col.getBlue();
					out.print(" new java.awt.Color(" + r + "," + g + "," + b + ")");
					i++;
					if (i == colors.length) {
						out.println();
					} else {
						out.println(',');
					}
				}
				out.println(" };");
				out.println(" public static final java.awt.Color get(int color) { return colors[color];}");
			}
			out.println(" }");

			out.println();
			out.println("public static final class Fonts {");
			if (fonts != null) {
				int i = 0;
				for (final Object[] cs : fonts) {
					final String name = (String) cs[0];
					out.print(" public static final int ");
					out.print(name);
					out.print('=');
					out.print(i);
					out.println(";");
					i++;
				}
				out.println("private static final java.awt.Font[] fonts = new java.awt.Font[]{");
				i = 0;
				for (final Object[] cs : fonts) {
					final String fontStr = (String) cs[1];
					final Font f = parseFont(fontStr);
					out.print(" new java.awt.Font(\"" + f.getName() + "\"," + f.getSize() + "," + f.getStyle() + ")");
					i++;
					if (i == fonts.length) {
						out.println();
					} else {
						out.println(',');
					}
				}
				out.println(" };");
				out.println(" public static final java.awt.Font get(int font) { return fonts[font];}");
			}
			out.println(" }");

			out.println();
			out.println("public static final class Resources {");
			if (fonts != null) {
				int i = 0;
				for (final Object[] cs : resources) {
					final String name = (String) cs[0];
					out.print(" public static final int ");
					out.print(name);
					out.print('=');
					out.print(i);
					out.println(";");
					i++;
				}
				out.println("private static final String[] resources = new String[]{");
				i = 0;
				for (final Object[] cs : resources) {
					final String str = (String) cs[1];
					out.print("\"" + str + "\"");
					i++;
					if (i == resources.length) {
						out.println();
					} else {
						out.println(',');
					}
				}
				out.println(" };");
				out.println(" public static final String get(int resource) { return resources[resource];}");
			}
			out.println(" }");
			// characters
			out.println();
			out.println("public static final class CharInfo {");
			if (chars != null) {
				int i = 0;
				for (final Object[] cs : chars) {
					final String name = (String) cs[0];
					out.print(" public static final int ");
					out.print(name);
					out.print('=');
					out.print(i);
					out.println(";");
					i++;
				}
				out.println("private static final String[] charinfo = new String[]{");
				i = 0;
				for (final Object[] cs : chars) {
					final String img = (String) cs[1];
					out.print('"');
					out.print(img);
					out.print('"');
					i++;
					if (i == chars.length) {
						out.println();
					} else {
						out.println(',');
					}
				}
				out.println(" };");
				out.println(" public static final String get(int character) { return charinfo[character];}");
			}
			out.println(" }");
			out.println(" }");
		}
	}

}
