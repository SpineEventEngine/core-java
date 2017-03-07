/*
 * Copyright 2017, TeamDev Ltd. All rights reserved.
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.spine3.net;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utility class for working with {@link InternetDomain}s.
 *
 * @author Alexander Yevsyukov
 */
public class InternetDomains {

    /**
     * Regular expression to match all IANA top-level domains.
     * List accurate as of 2010/02/05.  List taken from:
     * http://data.iana.org/TLD/tlds-alpha-by-domain.txt
     * Copied from {@code android.util.Patterns}.
     */
    private static final String TOP_LEVEL_DOMAIN_STR =
            "((aero|arpa|asia|a[cdefgilmnoqrstuwxz])"
            + "|(biz|b[abdefghijmnorstvwyz])"
            + "|(cat|com|coop|c[acdfghiklmnoruvxyz])"
            + "|d[ejkmoz]"
            + "|(edu|e[cegrstu])"
            + "|f[ijkmor]"
            + "|(gov|g[abdefghilmnpqrstuwy])"
            + "|h[kmnrtu]"
            + "|(info|int|i[delmnoqrst])"
            + "|(jobs|j[emop])"
            + "|k[eghimnprwyz]"
            + "|l[abcikrstuvy]"
            + "|(mil|mobi|museum|m[acdeghklmnopqrstuvwxyz])"
            + "|(name|net|n[acefgilopruz])"
            + "|(org|om)"
            + "|(pro|p[aefghklmnrstwy])"
            + "|qa"
            + "|r[eosuw]"
            + "|s[abcdeghijklmnortuvyz]"
            + "|(tel|travel|t[cdfghjklmnoprtvwz])"
            + "|u[agksyz]"
            + "|v[aceginu]"
            + "|w[fs]"
            + "|(xn\\-\\-0zwm56d|xn\\-\\-11b5bs3a9aj6g|xn\\-\\-80akhbyknj4f|xn\\-\\-9t4b11yi5a|xn\\-\\-deba0ad|xn\\-\\-g6w251d|xn\\-\\-hgbk6aj7f53bba|xn\\-\\-hlcj6aya9esc7a|xn\\-\\-jxalpdlp|xn\\-\\-kgbechtv|xn\\-\\-zckzah)"
            + "|y[etu]"
            + "|z[amw])";

    /**
     * Regular expression pattern to match all IANA top-level domains.
     */
    private static final Pattern TOP_LEVEL_DOMAIN = Pattern.compile(TOP_LEVEL_DOMAIN_STR);

    /**
     * Good characters for Internationalized Resource Identifiers (IRI).
     * This comprises most common used Unicode characters allowed in IRI
     * as detailed in RFC 3987.
     * Specifically, those two byte Unicode characters are not included.
     * Copied from {@code android.util.Patterns}, removing ID address part in the end.
     */
    private static final String GOOD_IRI_CHAR = "a-zA-Z0-9\u00A0-\uD7FF\uF900-\uFDCF\uFDF0-\uFFEF";

    private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile(
            "((([" + GOOD_IRI_CHAR + "][" + GOOD_IRI_CHAR + "\\-]*)*[" + GOOD_IRI_CHAR + "]\\.)+"
            + TOP_LEVEL_DOMAIN + ')');

    private InternetDomains() {
        // Prevent instantiation of this  utility class.
    }

    /**
     * Obtains {@code Pattern} for checking an Internet domain name.
     */
    public static Pattern pattern() {
        return DOMAIN_NAME_PATTERN;
    }

    private static void checkArgumentIsDomainName(CharSequence name) {
        checkArgument(pattern().matcher(name).matches());
    }

    /**
     * Creates a new {@code InternetDomain} instance for the passed name.
     *
     * @param name a valid Internet domain name
     * @return new {@code InternetDomain} instance
     * @throws IllegalArgumentException if the passed domain name is not valid
     */
    public static InternetDomain valueOf(CharSequence name) {
        checkNotNull(name);
        checkArgumentIsDomainName(name);

        final InternetDomain result = InternetDomain.newBuilder()
                                                    .setValue(name.toString())
                                                    .build();
        return result;
    }
}
