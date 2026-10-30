# ادامهٔ توسعه و آزمون اجرایی CarOBD — ۲۰۲۶/۱۰/۳۰

این گزارش ادامهٔ Sprint5 است؛ گزارش‌های تاریخی Sprint1 تا Sprint4 بازنویسی نشده‌اند. PASS فقط اجرای مشاهده‌شده است؛ PARTIAL یعنی بخشی پیاده‌سازی یا آزمایش شده؛ BLOCKED یعنی مانع محیطی مشخص؛ NOT TESTED یعنی شاهد اجرایی نداریم. نتیجهٔ شبیه‌ساز، اعتبارسنجی آداپتور یا خودرو نیست.

## وضعیت آغاز و محدوده

ریشهٔ واقعی Git: `/home/mohammad/Desktop/CarOBD/CarOBD`. شاخه `feat/car-obd-development` و remote برابر `git@github.com:SaraniMohammadReza/CarOBD.git` بود. HEAD آغاز این ادامه و remote پس از fetch هر دو `e387bf00df4106045c9484f5d5e301edde8886b5` بودند. تغییرات ناتمام Sprint5 در مدل سفر، دیتابیس، پیش‌نمایش ضبط، منابع و مستندات حفظ شدند. `report.md` فایل شخصی کاربر است و وارد کامیت نمی‌شود؛ SHA256 آن `cc39511e9fccbc8f31fcb15fcf84e1927d85695058e1af72fe8810cc58cf4325` است. تغییر قبلی کاربر در پلاگین Kotlin فایل ریشه حفظ شده است. هیچ reset، clean گیت، force push یا تغییر master انجام نشد.

## Android Studio و محیط پایدار — PASS

لاگ واقعی IDE برای ریشهٔ درست، `onSuccess(RESOLVE_PROJECT:0)` و `onImportFinished` را در ساعت 14:38:40 و بار دیگر 16:35:12 ثبت کرده است. زمان‌ها همان زمان ثبت‌شدهٔ IDE هستند. بار دوم ۲۷ ثانیه و تنظیم IDE در حالت offline بود. این نتیجه صرفاً از موفقیت CLI استنباط نشده است. شاهد پالایش‌شده بدون اطلاعات خصوصی IDE: `/home/mohammad/.local/share/carobd-build/runtime-qa/studio-sync.log`.

کش عادی `/home/mohammad/.gradle` مشترک بین CLI و Studio است. اسکریپت پایدار `/home/mohammad/.gradle/init.d/carobd-repositories.gradle` مسیر Google Maven را برای همین پروژه از mirror موجود Aliyun عبور می‌دهد و مسیرهای اصلی Central/Plugin Portal را حفظ می‌کند. مختصات یا نسخه‌ها عوض نشده‌اند؛ هیچ JAR/AAR دستی به کش کپی نشده و هیچ کش، SDK، JDK یا توزیع Gradle وارد Git نشده است. اسکریپت `--init-script` در خط فرمان لازم نیست.

Gradle wrapper9.8.1، AGP9.3.3، Kotlin/Compose2.2.10، coroutines1.10.1، compile/target37، min24 و target JVM17 حفظ شدند. CLI با Temurin24.0.2+12 در `/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12` اجرا می‌شود. daemon موفق IDE از `/home/mohammad/.gradle/jdks/eclipse_adoptium-24-amd64-linux.2` استفاده کرد؛ فایل release همان Temurin24.0.2+12 را تأیید می‌کند. معیار JVM کامیت‌شده Gradle نسخه24 است؛ این با هدف کامپایل17 تفاوت دارد. SDK در `/home/mohammad/Android/Sdk` است؛ API37 با نام بستهٔ جدید `platforms;android-37.0` و build-tools37.0.0 حاضرند. راهنمای انتخاب JDK و بازکردن ریشهٔ صحیح: [ANDROID_STUDIO_SETUP.md](ANDROID_STUDIO_SETUP.md).

warm-up آفلاین، ۱۳۳ configuration دارای artifact، ۲۳۳۱ رخداد artifact و ۵۶ bucket داخلی metadata را بدون خطا بررسی کرده است. debug/release، unit/instrumentation، lint، compiler، buildscript و providerها پوشش داده شده‌اند. bucketهای metadata بدون نسخهٔ مستقل از طریق classpath واقعی و BOM حل شده‌اند؛ خروجی project توسط task ساخته می‌شود. اجرای اولیهٔ maintenance چهار خطای انتخاب variant/metadata داشت؛ پس از اصلاح خود task نگهداری، اجرای offline صفر خطا داشت. این خطاها به‌عنوان موفقیت ثبت نشده‌اند.

## نقشهٔ اجرا و مرزهای معماری

`CarObdApp` ماژول‌های Koin را بارگذاری می‌کند؛ `MainActivity` زبان ذخیره‌شده را پیش از composition اعمال می‌کند و `MainAppScaffold` گراف Compose را می‌سازد. ViewModelها از repository/use-case استفاده می‌کنند. `ObdRepositoryImpl` مالک نشست است؛ `DiagnosticSession` dispatch، initialization، capability و polling را به core:adapter/protocol/telemetry وصل می‌کند. `TransportFactory` فقط transport انتخاب‌شده را می‌سازد؛ simulator یک delegate مستقل است.

| صفحه/مسیر | مسیر اصلی داده و کنش |
|---|---|
| dashboard | DashboardViewModel → ObserveTelemetry/Connect use cases → repository → session/producer |
| connect | ConnectionCenterViewModel → profile repository + ObdRepository → admission/session/test evidence |
| diagnostics | DiagnosticsViewModel → ScanDtc/ClearDtc use cases → read-only dispatcher؛ پاک‌سازی فقط simulator |
| evidence و زیرصفحه‌ها | Readiness/MonitorTests/FreezeFrame/VehicleInformation/DtcHistory ViewModel → repository → core decoder یا SQLite |
| recordings | RecordingViewModel → RecordingController/FileTranscriptStore؛ replay مستقل از physical delegate |
| trip | TripViewModel/ManualTripLogger → TripComputer → evidence؛ سفر خودکار در SQLite |
| maintenance | MaintenanceViewModel → metadata کاربر و سابقهٔ سرویس؛ توصیهٔ OEM تأییدشده نیست |
| ecu_profiles | VehicleProfileViewModel → catalogue/selection؛ انتخاب کاتالوگ اثبات هویت ECU نیست |
| simulator | SimulatorSelectorViewModel → profile صریح → IsolatedSimulatorTransport |
| settings و adapter_inspector | DataStore برای زبان/تم/واحد/polling؛ اطلاعات گزارش‌شده و راهنمای محدودیت شواهد |
| terminal | TerminalViewModel → SendRawCommandUseCase → allowlist نهایی؛ مسیر از منوی بیشتر در دسترس است |

## اصلاحات واقعی

- پنجرهٔ بلوتوث داشبورد دیگر قبل از بازشدن به دلیل نبود مجوز رد نمی‌شود. ابتدا پنجره باز می‌شود و بازیابی مجوز در همان جریان ممکن است. مسیر داشبورد و ویرایشگر از انتخابگر مشترک استفاده می‌کنند.
- انتخابگر Classic و BLE جداست؛ حالت شروع‌نشده، جست‌وجو، توقف، خطا، پایان مهلت بدون دستگاه و انتخاب نشانی روشن است. اسکن حداکثر۱۰ ثانیه است؛ بستن پنجره/خروج از foreground آن را لغو می‌کند. انتخاب صرفاً با لمس صریح کاربر است و نشانی دقیق حفظ می‌شود؛ هیچ دستگاه یا اتصال تصادفی اضافه نشده است.
- کنترل مجوزها برای ویژگی مربوط قابل محدودکردن است. توضیح نیاز به Nearby devices، اعلان و شبکهٔ محلی اضافه شد. unsupported/off/denied/permanent/settings recovery جدا هستند. در Android قدیمی خاموش‌بودن location service برای discovery به پیام و Settings مناسب منتهی می‌شود؛ اجرای آن روی API قدیمی هنوز NOT TESTED است.
- مجوز `ACCESS_LOCAL_NETWORK` برای TCP آداپتور در target37 به manifest، runtime flow و admission پیش از ساخت transport افزوده شد. این مسیر به adapter بلوتوث وابسته نیست. درخواست ADVERTISE اضافه نشده است. اسکن یا اتصال شبکهٔ خودکار اضافه نشده است.
- داشبورد دیگر با سه ردیف مجوز شروع نمی‌شود؛ کنترل‌ها در مرکز اتصال و انتخابگر ویژگی هستند. چهار مقدار اصلی در شبکهٔ۲×۲ با واحد، مقدار نامشخص و وضعیت stale نمایش داده می‌شوند.
- نوار پایین به چهار مقصد اصلی و «بیشتر» تبدیل شد؛ اندازهٔ متن و فضای لمس مناسب‌تر است. لمس زبانه، مقصد اصلی آن را باز می‌کند. ترمینال پیش‌تر route داشت ولی ورودی منو نداشت؛ اکنون در بیشتر قرار دارد.
- فرم شروع و گزینه‌های transport با FlowRow و scroll از شکستن عمودی گزینه‌ها جلوگیری می‌کنند. متن فارسی ادعای دقت محاسبات از metadata را حذف می‌کند. غلط «خودروی تشخه» اصلاح شد؛ coolant و نام‌گذاری راهنمای آداپتور اصلاح شد. ادعاهای cloud-ready/۱۰۰+سنسور/همهٔ خودروها و تأیید اصالت تراشه از صفحهٔ درباره/راهنما حذف شدند.
- مقادیر شبیه‌ساز از snapshot مشترک هر چرخه ساخته می‌شوند؛ ATI و درخواست‌های metadata زمان موتور را جلو نمی‌برند. سرعت، RPM، MAF، load و throttle هم‌خوان‌ترند. bitmapهای Mode01 فقط PID واقعاً پاسخ‌گو را اعلام می‌کنند. سناریوی ECU ساکت از موتور خاموشِ پاسخ‌گو و موتور درجا جدا شده است. fault scenarios قبلی حفظ شدند.
- برچسب simulation در گراف اصلی نمایش داده می‌شود؛ متن سناریو با زبان انتخابی است و شناسهٔ فنی جدا باقی می‌ماند. منبع و زمان شروع آخرین اسکن DTC نیز به نتیجه متصل است تا پس از قطع اتصال، نتیجهٔ شبیه‌سازی بی‌منبع نماند. UI پاک‌سازی فقط در نشست simulator فعال است؛ dispatch نهایی همچنان physical04 را رد می‌کند.
- رنگ‌های سطح Material با palette برنامه هماهنگ شدند؛ متن دکمهٔ تم روشن با رنگ primary تضاد مناسب‌تری دارد. انتخاب تم semantics رادیویی و حداقل ارتفاع۴۸dp دارد؛ زبان با دو گزینهٔ نام‌دار و وضعیت انتخاب صریح تعیین می‌شود. آزمون کامل TalkBack/کنتراست همهٔ صفحات هنوز PARTIAL است.
- سفر جدید امتیازهای رانندگی ناموجود را null نگه می‌دارد. ترتیب canonical شواهد پس از بازکردن SQLite حفظ می‌شود. پیش‌نمایش ضبط محدود و metadata قابل مشاهده است؛ export دادهٔ اصلی را کوتاه نمی‌کند.

## آزمون اجرایی و شواهد

AVD رسمی موجود `Pixel_10` با Android17/API37، تصویر `google_apis_playstore_ps16k/x86_64`، emulator37.1.11 و KVM اجرا شد. اندازهٔ اصلی1080×2424 و font scale1.0 بود. APK جدید نصب و MainActivity واقعاً اجرا شد. سرور عادی ADB توسط IDE مرتب restart می‌شد؛ QA از سرور مستقل محلی5038 و فقط serial `emulator-5554` استفاده کرد. هیچ دستگاه فیزیکی انتخاب نشد. این جداسازی به Gradle Sync یا dependency routing وابسته نیست.

آزمون Compose/Espresso موجود روی API37 به reflection حذف‌شدهٔ `InputManager.getInstance` برخورد کرد. بدون تغییر نسخه‌ها یا حذف پوشش، آزمون پیمایش با API رسمی platform UiAutomation و درخت دسترس‌پذیری اجرا شد. آزمون قالب قدیمی نیز نام بستهٔ اشتباه `com.sarani.myapplication` داشت؛ انتظار به applicationId واقعی اصلاح شد. سه آزمون SQLite از اولین اجرای دستگاه گذشتند.

دور فارسی با اندازهٔ900×2000 و فونت1.3،۵ آزمون با صفر شکست/خطا/skip داشت: مهاجرت DTC، دو آزمون بازیابی سفر، applicationId و یک جریان بزرگ پیمایش/مجوز/شبیه‌ساز. این عدد تعداد متد تست است، نه تعداد صفحه یا ادعای چندصد تست UI. نتایج نهایی آخرین source در بخش پایانی ثبت می‌شوند. شکست‌های آزمایش‌سازی (اسکرول، تشابه نام دکمه با tab، پردهٔ اعلان) موفقیت شمرده نشده‌اند.

تصاویر و XML/log خارج Git در `/home/mohammad/.local/share/carobd-build/runtime-qa/` قرار دارند. زیرپوشهٔ `carobd-runtime-qa` تصاویر زبان‌ها را با پیشوند fa/en نگه می‌دارد. تصاویر اولیهٔ بدون پیشوند صرفاً شواهد کشف نقص‌اند و شاهد محصول نهایی نیستند.

بررسی تکمیلی با ADB/uiautomator روی همین شبیه‌ساز، ماندگاری هر دو زبان پس از force-stop و شروع دوبارهٔ فرایند، هر سه تب inspector/help، انتخاب Eco و بازگشت Normal، گزارش موفق Test Connection برای profile صریح SIMULATOR، ضبط/ذخیره/انتخاب transcript و replay در حال اتصال simulator و سپس قطع نشست را نیز اجرا کرد. گزارش wizard به‌طور واقعی نشست آزمون را بسته بود. نتیجهٔ replay از نشست فیزیکی استخراج نشده است. خروجی scriptها در manual-smoke.log و capture-smoke-resume.log ثبت شده است؛ اجرای نخست script دوم به دلیل ندیدن متن گزارش خارج viewport متوقف شد و پس از اسکرول ادامه یافت، نه اینکه شکست اولیه PASS شمرده شود.

پوشش مشاهده‌شده شامل راه‌اندازی، داشبورد قطع، مرکز اتصال، اعلان سیستم، Nearby devices سیستم، رد و درخواست دوباره و اعطای مجوز، ویرایشگرهای Classic/BLE/Wi-Fi/USB، پایان مهلت اسکن Classic بدون دستگاه، diagnostics، readiness/evidence، Mode06، history، recording، freeze-frame، Mode09، settings، maintenance، trip، catalogue، terminal، simulator، اتصال و قطع شبیه‌ساز است. کشف دستگاه واقعی، RFCOMM/GATT واقعی و permission USB فیزیکی از این شواهد نتیجه نمی‌شود.

## محدودیت‌ها و اولویت‌ها

- Physical adapter و real vehicle: NOT TESTED؛ هیچ اجازهٔ تست خودرو در این sprint وجود ندارد. allowlist، physical04/security/write/programming denial، token ownership، cancellation/quarantine و replay isolation حفظ می‌شوند.
- Bluetooth در شبیه‌ساز فقط مسیر مجوز و empty/timeout را تأیید می‌کند؛ دستگاه واقعی یا سازگاری BLE service اثبات نشده است. USB و TCP محلی بدون endpoint واقعی PARTIAL هستند.
- رابط راهنمای آداپتور و catalogue دادهٔ مرجع است، نه گواهی سازگاری یا سلامت خودرو. metadata کاربر نیز سنسور نیست؛ مقادیر نمونهٔ فرم باید بازبینی شوند.
- فارسی/انگلیسی و placeholderها بررسی می‌شوند؛ catalogue تخصصی، جزئیات فنی، OEM units و متن برخی خطاهای developer همچنان نیاز به بازبینی جدا دارند. هیچ ادعای کامل‌بودن ترجمهٔ تمام کاتالوگ یا TalkBack همهٔ صفحات نداریم.
- Mode05 و تفسیر OEM Mode06، پروتکل‌های تخصصی/USB تأییدنشده، custom PID، auto reconnect و cloud sync از این کار نتیجه نمی‌شوند. capability نامشخص پشتیبانی محسوب نمی‌شود.
- موفقیت build، تست و emulator به معنی production-ready برای خودرو نیست. اولویت بعدی bench فقط‌خواندنی با اجازهٔ مستقل، سپس اعتبارسنجی جداگانهٔ خودرو و تکمیل تجربهٔ دسترس‌پذیری/کاتالوگ است.

## منابع رسمی

[مجوزهای Bluetooth](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)، [مجوز اعلان](https://developer.android.com/develop/ui/compose/notifications/notification-permission)، [نوع سرویس foreground](https://developer.android.com/develop/background-work/services/fgs/service-types) و [مجوز شبکهٔ محلی Android17](https://developer.android.com/privacy-and-security/local-network-permission) مبنای بررسی platform بوده‌اند. مستند شبکهٔ محلی الزام target37 را تأیید می‌کند؛ دسترسی عمومی اینترنت جای مجوز LAN را نمی‌گیرد.

## نتیجهٔ نهایی build و آزمون‌ها

نسخهٔ نهایی شامل اصلاح جمعِ تعداد دستگاه‌هاست؛ `qa_scan_found` اکنون plurals واقعی دارد. بررسی منابع **۴۱۰** ورودی متن/جمع/آرایهٔ EN/FA را با کلید و امضای قالب هم‌سان تأیید می‌کند. هشدار تازهٔ جمع رفع شد؛ هیچ lint rule یا تستی برای سبزکردن gate غیرفعال نشده است.

فرمان نهایی از ریشهٔ پروژه، با `JAVA_HOME=/home/mohammad/.local/share/carobd-build/jdks/jdk-24.0.2+12` و `ANDROID_HOME=/home/mohammad/Android/Sdk`:

```bash
./gradlew --offline clean \
  :app:assembleDebug :app:testDebugUnitTest :app:checkDebugDuplicateClasses :app:lintDebug \
  :core:model:test :core:adapter:test :core:protocol:test :core:telemetry:test :core:vehicle:test \
  :app:assembleDebugAndroidTest --console=plain
```

**PASS — exit0، ۱دقیقه۳۴ثانیه، ۱۰۷ task اجراشده از clean.** فایل شاهد `runtime-qa/final-offline-gate.log` در ریشهٔ شواهد خارجی است. هیچ `--init-script` یا GRADLE_USER_HOME موقت استفاده نشد.

| مجموعهٔ JVM | tests | failures/errors/skipped |
|---|---:|---|
| app |124|0/0/0|
| core:model |48|0/0/0|
| core:adapter |110|0/0/0|
| core:protocol |125|0/0/0|
| core:telemetry |19|0/0/0|
| core:vehicle |1|0/0/0|
| مجموع |**427**|**0/0/0**|

۲۲ مورد `ObdRepositorySafetyTest` از همین مجموعه، integration درون‌فرایندی با transportهای کنترل‌شده‌اند؛ دوباره به مجموع۴۲۷ اضافه نمی‌شوند. سه آزمون integration واقعی SQLite روی Android، یک آزمون applicationId و یک جریان بزرگ UI، پنج متد آزمون اندرویدی را تشکیل می‌دهند. تکرار همان پنج متد در دو زبان به معنی ده متد متفاوت نیست. script میزبان SQLite نیز مسیرهای DTC/trip و rollback/reopen را جداگانه تأیید کرد؛ این اجرای میزبان جای Android را نمی‌گیرد.

**lint: PASS با۷۹ Warning و صفر Error.** دسته‌ها: UnusedResources50، TypographyEllipsis15، ModifierParameter7، UseKtx4، PluralsCandidate1، RedundantLabel1، ObsoleteSdkInt1. هشدار plural باقی‌مانده رشتهٔ قدیمی و بدون مصرف `dtc_codes_found` است؛ هشدار جدید انتخابگر برطرف شده است. هشدارهای بررسی نسخهٔ آنلاین در اجرای offline ظاهر نمی‌شوند، بنابراین کاهش تعداد نسبت به گزارش‌های آنلاین به معنی رفع همهٔ هشدارهای قدیمی نیست. transaction دیتابیس برای حفظ رفتار atomic به تبدیل صوری صرفاً جهت حذف UseKtx تغییر نکرد. گزارش XML/HTML کامل در `final-lint.xml/html` موجود است. duplicate classes و assembleDebug هر دو PASS هستند.

دور فارسیِ اندازهٔ کوچک/فونت1.3 با `connectedDebugAndroidTest`، exit0 و پنج آزمون بدون failure/error/skip، ۲دقیقه۳۸ثانیه طول کشید. اندازه و فونت بعد از آن به1080×2424 و1.0 برگشت. فرمان suite انگلیسی نهایی:

```bash
ANDROID_ADB_SERVER_PORT=5038 ADB_SERVER_SOCKET=tcp:localhost:5038 \
  ./gradlew --offline :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.qaLocale=en --console=plain
```

این suite آزمون first-launch و denial را روی **نصب تازهٔ debug در شبیه‌ساز ایزوله** فرض می‌کند. آن را روی گوشی/دادهٔ مالک اجرا نکنید. پس از smoke دستی، اجرای نخست suite نهایی به دلیل مجوز اعلانِ از قبل داده‌شدهٔ fixture دستی متوقف شد؛ فایل `final-ui-existing-fixture-failure.log` این مورد را نگه می‌دارد. fixture توسط چرخهٔ عادی UTP پایان یافت و suite روی نصب تازه تکرار شد؛ این توقف به‌عنوان PASS یا خطای آداپتور گزارش نشده است.

APK نهایی: `/home/mohammad/Desktop/CarOBD/CarOBD/app/build/outputs/apk/debug/app-debug.apk`، اندازه۲۲٬۰۴۶٬۴۸۴ بایت؛ package `com.sarani.carobd`، version1.0.0/code1، min24/target37. `apksigner verify` موفق و امضای debug v2 با یک signer تأیید شده است. این APK انتشار production نیست.

```text
SHA256=f317bfa500e5a2e3d4049881d49cfc4c643c054e9948065a88acc03c38ae0a82
```

## وضعیت‌های محیط در پایان

```text
GRADLE_USER_HOME=/home/mohammad/.gradle (effective default; no temporary user home)
GRADLE_JDK=Temurin 24.0.2+12; CLI local installation / IDE Gradle-managed matching installation
ANDROID_SDK=/home/mohammad/Android/Sdk
WRAPPER_CACHED=YES (9.8.1 binary distribution; IDE Kotlin DSL source archive also cached)
PLUGIN_DEPENDENCIES_CACHED=YES (offline configuration, build and broad artifact resolution passed)
PROJECT_DEPENDENCIES_CACHED=YES (133 artifact-bearing configurations, 2331 artifact occurrences)
OFFLINE_HELP_STATUS=PASS (exit0, 1s, no --init-script)
OFFLINE_BUILD_STATUS=PASS (clean full JVM/lint/duplicate/debug/test-APK gate, exit0)
ANDROID_STUDIO_SYNC_READY=YES (actual IDE onSuccess + import completion observed twice)
REPOSITORY_ROUTING_USED=user ~/.gradle/init.d/carobd-repositories.gradle; scoped Aliyun Google mirror plus original fallbacks
MISSING_ARTIFACTS=NONE in executed broad offline resolution and final gates
```

cache warm-up نهایی دوباره با `--offline -PcarobdCacheWarmup=true carobdWarmAllConfigurations --no-configuration-cache` اجرا شد: exit0،۲ثانیه،۱۳۳ configuration،۲۳۳۱ artifact occurrence،۵۶ bucket metadata،صفر failure. `offline-help.log` و `offline-cache.log` خارج Git حفظ شده‌اند. برای Studio همان wrapper و Gradle user home را نگه دارید؛ اگر انتخاب JDK خواست، Temurin24 در مسیر بالا یا JVM24 منطبق با daemon criteria را انتخاب کنید؛ target17 را به‌عنوان Gradle runtime انتخاب نکنید. SDK همان مسیر بالا است. Sync آماده است؛ مدیریت دستگاه/Run از داخل IDE با مشکل restart سرور ADB عادی5037 جداگانه باقی می‌ماند و PASS آن ادعا نشده است. QA اجرایی از5038 استفاده کرده است.

## جدول صریح حدود تأیید

| مورد | وضعیت و محدوده |
|---|---|
| Studio Sync و کش مشترک | PASS واقعی IDE؛ configuration و build آفلاین نیز PASS |
| JVM safety/protocol/repository | PASS؛۴۲۷ آزمون، شامل۲۲ آزمون repository درون همین مجموع |
| Android SQLite | PASS در Android17 emulator؛۳ متد migration/recreation |
| permission اعلان و Bluetooth | PASS درخواست سیستم، denial، درخواست دوباره و grant در emulator |
| Bluetooth off و empty/timeout/cancel | PASS در emulator؛ کشف/اتصال آداپتور واقعی NOT TESTED |
| permanent denial، برگشت از Settings، revoke حین اتصال | پیاده‌سازی موجود؛ مسیرهای کامل سیستم PARTIAL/NOT TESTED در این دور |
| BLE/USB/TCP | انتخابگر/فرم PASS؛ GATT/RFCOMM/USB permission/TCP endpoint فیزیکی NOT TESTED |
| Android17 local-network permission | manifest/runtime/admission پیاده‌سازی و compile شده؛ LAN endpoint واقعی NOT TESTED |
| simulator | PASS حرکت، موتور خاموش پاسخ‌گو، اسکن، قطع/اتصال دوباره، foreground/background و اعلان |
| fault scenarios | fixtureهای unit PASS؛ همهٔ سناریوهای خطا به‌صورت جداگانه در GUI اجرا نشده‌اند |
| Mode02/09/06/readiness | خواندن واقعی از simulator PASS؛ انطباق OEM/خودرو NOT TESTED |
| Test Connection wizard | PASS برای profile صریح SIMULATOR، گزارش مراحل و بسته‌شدن نشست آزمون |
| recording/replay | PASS ضبط/ذخیره/انتخاب و replay محلی در حال اتصال simulator؛ SAF export/import GUI و capture-tail process death NOT TESTED |
| EN/FA، RTL/LTR، تم و فونت | PASS در مسیرهای ثبت‌شده؛ کاتالوگ کامل، همهٔ چهار تم، TalkBack و contrast exhaustive PARTIAL |
| زبان پس از process restart | PASS با force-stop و شروع مجدد برای هر دو زبان؛ تصاویر smoke موجود |
| APK و نصب/اجرای emulator | PASS debug؛ نسخهٔ انتشار/فروشگاهی از این کار نتیجه نمی‌شود |
| physical adapter / vehicle | NOT TESTED؛ حفاظت physical04/08/security/write/programming باقی است |


## اجرای نهایی اندروید، تصاویر و Git

**PASS — suite انگلیسی روی APK نهایی:** exit0،۲دقیقه۳۸ثانیه،۵ tests،۰ failure،۰ error،۰ skipped. شاهد `final-ui-gate.log` و `final-android-results.xml` است. source این APK کامیت `bc7b6f0` است؛ hash و اندازهٔ بالا از همان APK تازه محاسبه شده‌اند. پس از پایان suite و چرخهٔ عادی UTP، همین APK دوباره روی `emulator-5554` نصب و MainActivity اجرا شد. تصویر `final-owner-first-launch.png` صفحهٔ راه‌اندازی اولیهٔ فارسی را نشان می‌دهد. هیچ نشست فیزیکی/شبیه‌ساز در این وضعیت فعال نیست و مالک باید metadata واقعی خود را بازبینی و وارد کند. `final-installed-apk-logcat.log` فرایند تازه را بدون FATAL EXCEPTION ثبت می‌کند. اندازه1080×2424 و font scale1.0 بازگردانده شده‌اند.

نمونه‌های مهم شواهد تصویری در ریشهٔ `/home/mohammad/.local/share/carobd-build/runtime-qa/`:

- `carobd-runtime-qa/en-dashboard-simulation.png`: داشبورد جاری و نشان simulation؛ `fa-dashboard-simulation.png` همان مسیر در اندازهٔ کوچک/فونت1.3.
- `carobd-runtime-qa/fa-classic-picker-result.png` و `fa-classic-picker-canceled.png`: پایان بدون دستگاه و لغو صریح؛ `fa-bluetooth-disabled.png`: وضعیت off.
- `carobd-runtime-qa/en-notification-system-request.png` و `en-bluetooth-denied.png`: پنجرهٔ واقعی سیستم و denial؛ تصاویر grant نیز موجودند.
- `carobd-runtime-qa/fa-settings-daylight.png`: تم روشن و فونت1.3؛ `en-language-changed-and-recreated.png`: زبان پس از بازسازی Activity.
- `carobd-runtime-qa/en-readiness-loaded.png`، `en-mode09-loaded.png`، `en-freeze-frame-loaded.png` و `en-mode06-loaded.png`: مسیرهای خواندن واقعی simulator؛ نسخه‌های fa نیز موجودند.
- `carobd-runtime-qa/en-background-foreground-notification.png`: اعلان واقعی foreground هنگام background با برچسب simulator.
- `smoke-english-process-restart.png` و `smoke-persian-process-restart.png`: ماندگاری زبان پس از توقف کامل فرایند.
- `smoke-connection-help.png` و `smoke-motorcycle-help.png`: تب‌های راهنما؛ `smoke-simulator-test-connection-report.png`: wizard/test report.
- `smoke-saved-transcript.png` و `smoke-transcript-replay.png`: ذخیره و بازپخش محلی؛ `smoke-capture-disconnected.png`: قطع بعد از آن.

در زمان تحویل۱۱۳ فایل PNG شامل تصاویر کشف نقص و دورهای میانی ثبت شده‌اند؛ عکس بدون پیشوند یا تصویر شکست اولیه شاهد محصول نهایی محسوب نمی‌شود. نام‌های نهایی en/fa بالا به آخرین دور مربوط‌اند. JSON خلاصهٔ شمارش‌ها و APK در `final-result-summary.json` و XML تمام تست‌های JVM در `unit-results/` حفظ شده‌اند. فهرست و SHA256 شواهد در artifact-manifest.json موجود است. فایل‌های بزرگ/کش/تصاویر به Git اضافه نشده‌اند.

کامیت‌های این ادامه، همگی روی development و بدون force push:

| کامیت | تغییر |
|---|---|
| `d81e991` | null بودن معیارهای رانندگی ناموجود و ترتیب شواهد سفر پس از SQLite recreation |
| `f36f7eb` | bitmap پشتیبانی صحیح و snapshotهای هم‌خوان موتور شبیه‌سازی‌شده |
| `0081120` | مجوزها و LAN admission، انتخابگر، ناوبری/تم/زبان، منشأ اسکن و آزمون اجرایی |
| `bc7b6f0` | plurals درست برای شمار دستگاه‌های کشف‌شده |
| کامیت تحویل مستندات | همین گزارش، راهنمای محیط و inventory؛ HEAD نهایی در پیام تحویل و `final-git-state.txt` ثبت می‌شود |

کامیت‌های پیشین همین Sprint5 که پیش از این ادامه push شده بودند حفظ شدند: `1e166e5`، `18a3e51`، `3b38cd8`، `af047dd`، `cc6071b` و `e387bf0`. گزارش‌های Sprint1–4 تاریخی‌اند. این کار master را ایجاد/ویرایش/حذف نکرد؛ در clone حاضر فقط development و tracking آن وجود دارد. تغییر نهایی tracked شامل source فوق و مستندات تحویل است؛ `report.md` همچنان untracked و با SHA256 آغازین باقی می‌ماند. تطبیق HEAD محلی و remote پس از push در شاهد خارجی نهایی ثبت می‌شود؛ نتیجهٔ push فقط پس از تطبیق اعلام خواهد شد.
