package tools.system;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

//http://msdn.microsoft.com/en-ie/goglobal/bb896001.aspx
//Only support windows 7 currently
public class LocaleUtils {
  private static final String localeFileName = "localeFile.xml";

  public enum SupportLocales {
    //a
    af, af_ZA, am, am_ET, ar, ar_AE, ar_BH, ar_DZ, ar_EG, ar_IQ, ar_JO, ar_KW, ar_LB, ar_LY, ar_MA,
    ar_OM, ar_QA, ar_SA, ar_SY, ar_TN, ar_YE, arn, arn_CL, as, as_IN, az, az_Cyrl, az_Cyrl_AZ, az_Latn, az_Latn_AZ,
    //b
    ba, ba_RU, be, be_BY, bg, bg_BG, bn, bn_BD, bn_IN, bo, bo_CN, br, br_FR, bs, bs_Cyrl, bs_Cyrl_BA, bs_Latn, bs_Latn_BA,
    //c
    ca, ca_ES, co, co_FR, cs, cs_CZ, cy, cy_GB,
    //d
    da, da_DK, de, de_AT, de_CH, de_DE, de_LI, de_LU, dsb, dsb_DE, dv, dv_MV,
    //e
    el, el_GR, en, en_029, en_AU, en_BZ, en_CA, en_GB, en_IE, en_IN, en_JM, en_MY, en_NZ, en_PH, en_SG, en_TT, en_US,
    en_ZA, en_ZW, es, es_AR, es_BO, es_CL, es_CO, es_CR, es_DO, es_EC, es_ES, es_GT, es_HN, es_MX, es_NI, es_PA, es_PE,
    es_PR, es_PY, es_SV, es_US, es_UY, es_VE, et, et_EE, eu, eu_ES,
    //f
    fa, fa_IR, fi, fi_FI, fil, fil_PH, fo, fo_FO, fr, fr_BE, fr_CA, fr_CH, fr_FR, fr_LU, fr_MC, fy, fy_NL,
    //g
    ga, ga_IE, gd, gd_GB, gl, gl_ES, gsw, gsw_FR, gu, gu_IN,
    //h
    ha, ha_Latn, ha_Latn_NG, he, he_IL, hi, hi_IN, hr, hr_BA, hr_HR, hsb, hsb_DE, hu, hu_HU, hy, hy_AM,
    //i
    id, id_ID, ig, ig_NG, ii, ii_CN, is, is_IS, it, it_CH, it_IT, iu, iu_Cans, iu_Cans_CA, iu_Latn, iu_Latn_CA,
    //j
    ja, ja_JP,
    //k
    ka, ka_GE, kk, kk_KZ, kl, kl_GL, km, km_KH, kn, kn_IN, ko, ko_KR, kok, kok_IN, ky, ky_KG,
    //l
    lb, lb_LU, lo, lo_LA, lt, lt_LT, lv, lv_LV,
    //m
    mi, mi_NZ, mk, mk_MK, ml, ml_IN, mn, mn_Cyrl, mn_MN, mn_Mong, mn_Mong_CN, moh, moh_CA, mr, mr_IN, ms, ms_BN, ms_MY, mt, mt_MT,
    //n
    nb, nb_NO, ne, ne_NP, nl, nl_BE, nl_NL, nn, nn_NO, no, nso, nso_ZA,
    //o
    oc, oc_FR, or, or_IN,
    //p
    pa, pa_IN, pl, pl_PL, prs, prs_AF, ps, ps_AF, pt, pt_BR, pt_PT,
    //q
    qut, qut_GT, quz, quz_BO, quz_EC, quz_PE,
    //r
    rm, rm_CH, ro, ro_RO, ru, ru_RU, rw, rw_RW,
    //s
    sa, sa_IN, sah, sah_RU, se, se_FI, se_NO, se_SE, si, si_LK, sk, sk_SK, sl, sl_SI, sma, sma_NO, sma_SE, smj, smj_NO,
    smj_SE, smn, smn_FI, sms, sms_FI, sq, sq_AL, sr, sr_Cyrl, sr_Cyrl_BA, sr_Cyrl_CS, sr_Cyrl_ME, sr_Cyrl_RS, sr_Latn, sr_Latn_BA,
    sr_Latn_CS, sr_Latn_ME, sr_Latn_RS, sv, sv_FI, sv_SE, sw, sw_KE, syr, syr_SY,
    //t
    ta, ta_IN, te, te_IN, tg, tg_Cyrl, tg_Cyrl_TJ, th, th_TH, tk, tk_TM, tn, tn_ZA, tr, tr_TR, tt, tt_RU, tzm, tzm_Latn, tzm_Latn_DZ,
    //u
    ug, ug_CN, uk, uk_UA, ur, ur_PK, uz, uz_Cyrl, uz_Cyrl_UZ, uz_Latn, uz_Latn_UZ,
    //v
    vi, vi_VN,
    //w
    wo, wo_SN,
    //x
    xh, xh_ZA,
    //y
    yo, yo_NG,
    //z
    zh, zh_CN, zh_HK, zh_Hans, zh_Hant, zh_MO, zh_SG, zh_TW, zu, zu_ZA;

    public final String getName() {
      return super.name().replace("_", "-");
    }

    @Override
    public final String toString() {
      return getName();
    }

    public static final SupportLocales findValue(String value) {
      for (SupportLocales s : SupportLocales.values()) {
        if (s.getName().equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) {
          if (value.contains("_") || value.contains("-")) {
            return s;
          }
          throw new IllegalArgumentException("The locale must contain region ex: 'zh-TW' not 'zh' only.");
        }
      }
      throw new IllegalArgumentException("Unsupport locale of " + value);
    }
  }

  public static final void changeSystemLocaleTo(String localeStr) throws IOException {
    changeSystemLocaleTo(SupportLocales.findValue(localeStr));
  }

  public static final void changeSystemLocaleTo(SupportLocales locale) throws IOException {
    tools.system.SystemUtils.executeCMDwithoutResult(
        "control intl.cpl,,/f:\"" +
            createLocaleFile(locale).getAbsolutePath() +
            "\"");
  }

  private static final File createLocaleFile(SupportLocales locale) throws IOException {
    final File localeFile = new File("." + tools.file.FileUtils.File_SEP + localeFileName).getAbsoluteFile();
    try {
      tools.file.FileUtils.fileDelete(localeFile, "");
      localeFile.createNewFile();
      List<String> contentList = new LinkedList<String>();
      contentList.add("<gs:GlobalizationServices xmlns:gs=\"urn:longhornGlobalizationUnattend\">");
      contentList.add("<gs:UserList><gs:User UserID=\"Current\"/></gs:UserList>");
      contentList.add("<gs:UserLocale><gs:Locale Name=\"" + locale.getName() + "\" SetAsCurrent=\"true\"/></gs:UserLocale>");
      contentList.add("</gs:GlobalizationServices>");
      tools.file.FileUtils.writeFileData(localeFile, false, contentList, true);
      return localeFile;
    } finally {
      tools.system.SystemUtils.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          try {
            sleep(500);
          } catch (InterruptedException e) {}
          tools.file.FileUtils.fileDelete(localeFile, "");
        }
      });

    }
  }
}
