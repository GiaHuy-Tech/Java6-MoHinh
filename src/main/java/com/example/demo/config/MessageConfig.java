package com.example.demo.config;

import java.time.Duration;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

@Configuration
public class MessageConfig implements WebMvcConfigurer{
	@Bean("messageSource")
	public MessageSource getMessageSource() {
		ReloadableResourceBundleMessageSource mSource = new ReloadableResourceBundleMessageSource();
		mSource.setBasenames("classpath:/i18n/lang");
		mSource.setDefaultEncoding("utf-8");
		return mSource;
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// TODO Auto-generated method stub
		LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
		localeChangeInterceptor.setParamName("lang");
		registry.addInterceptor(localeChangeInterceptor);
	}
	@Bean("localeResolver")
	public LocaleResolver getLocaleResolver() {
		CookieLocaleResolver localeResolver = new CookieLocaleResolver();
		localeResolver.setCookiePath("/");
		localeResolver.setCookieMaxAge(Duration.ofDays(10));
		localeResolver.setDefaultLocale(new Locale("vi"));
		return localeResolver;
	}
}
