-- 봇 계정들의 오늘 투표 데이터 초기화 스크립트
-- predata_bot_001@predata.io ~ predata_bot_300@predata.io 계정의 오늘 투표를 삭제합니다.

-- 트랜잭션 시작
START TRANSACTION;

-- 1. 오늘 날짜 기준 설정 (UTC 00:00)
SET @today = DATE(NOW());

-- 2. 삭제할 activity_id 목록을 임시 테이블에 저장
CREATE TEMPORARY TABLE temp_activities_to_delete AS
SELECT a.activity_id
FROM activities a
INNER JOIN members m ON a.member_id = m.member_id
WHERE m.email IN (
    -- predata_bot_001@predata.io ~ predata_bot_300@predata.io
    'predata_bot_001@predata.io', 'predata_bot_002@predata.io', 'predata_bot_003@predata.io',
    'predata_bot_004@predata.io', 'predata_bot_005@predata.io', 'predata_bot_006@predata.io',
    'predata_bot_007@predata.io', 'predata_bot_008@predata.io', 'predata_bot_009@predata.io',
    'predata_bot_010@predata.io', 'predata_bot_011@predata.io', 'predata_bot_012@predata.io',
    'predata_bot_013@predata.io', 'predata_bot_014@predata.io', 'predata_bot_015@predata.io',
    'predata_bot_016@predata.io', 'predata_bot_017@predata.io', 'predata_bot_018@predata.io',
    'predata_bot_019@predata.io', 'predata_bot_020@predata.io', 'predata_bot_021@predata.io',
    'predata_bot_022@predata.io', 'predata_bot_023@predata.io', 'predata_bot_024@predata.io',
    'predata_bot_025@predata.io', 'predata_bot_026@predata.io', 'predata_bot_027@predata.io',
    'predata_bot_028@predata.io', 'predata_bot_029@predata.io', 'predata_bot_030@predata.io',
    'predata_bot_031@predata.io', 'predata_bot_032@predata.io', 'predata_bot_033@predata.io',
    'predata_bot_034@predata.io', 'predata_bot_035@predata.io', 'predata_bot_036@predata.io',
    'predata_bot_037@predata.io', 'predata_bot_038@predata.io', 'predata_bot_039@predata.io',
    'predata_bot_040@predata.io', 'predata_bot_041@predata.io', 'predata_bot_042@predata.io',
    'predata_bot_043@predata.io', 'predata_bot_044@predata.io', 'predata_bot_045@predata.io',
    'predata_bot_046@predata.io', 'predata_bot_047@predata.io', 'predata_bot_048@predata.io',
    'predata_bot_049@predata.io', 'predata_bot_050@predata.io', 'predata_bot_051@predata.io',
    'predata_bot_052@predata.io', 'predata_bot_053@predata.io', 'predata_bot_054@predata.io',
    'predata_bot_055@predata.io', 'predata_bot_056@predata.io', 'predata_bot_057@predata.io',
    'predata_bot_058@predata.io', 'predata_bot_059@predata.io', 'predata_bot_060@predata.io',
    'predata_bot_061@predata.io', 'predata_bot_062@predata.io', 'predata_bot_063@predata.io',
    'predata_bot_064@predata.io', 'predata_bot_065@predata.io', 'predata_bot_066@predata.io',
    'predata_bot_067@predata.io', 'predata_bot_068@predata.io', 'predata_bot_069@predata.io',
    'predata_bot_070@predata.io', 'predata_bot_071@predata.io', 'predata_bot_072@predata.io',
    'predata_bot_073@predata.io', 'predata_bot_074@predata.io', 'predata_bot_075@predata.io',
    'predata_bot_076@predata.io', 'predata_bot_077@predata.io', 'predata_bot_078@predata.io',
    'predata_bot_079@predata.io', 'predata_bot_080@predata.io', 'predata_bot_081@predata.io',
    'predata_bot_082@predata.io', 'predata_bot_083@predata.io', 'predata_bot_084@predata.io',
    'predata_bot_085@predata.io', 'predata_bot_086@predata.io', 'predata_bot_087@predata.io',
    'predata_bot_088@predata.io', 'predata_bot_089@predata.io', 'predata_bot_090@predata.io',
    'predata_bot_091@predata.io', 'predata_bot_092@predata.io', 'predata_bot_093@predata.io',
    'predata_bot_094@predata.io', 'predata_bot_095@predata.io', 'predata_bot_096@predata.io',
    'predata_bot_097@predata.io', 'predata_bot_098@predata.io', 'predata_bot_099@predata.io',
    'predata_bot_100@predata.io', 'predata_bot_101@predata.io', 'predata_bot_102@predata.io',
    'predata_bot_103@predata.io', 'predata_bot_104@predata.io', 'predata_bot_105@predata.io',
    'predata_bot_106@predata.io', 'predata_bot_107@predata.io', 'predata_bot_108@predata.io',
    'predata_bot_109@predata.io', 'predata_bot_110@predata.io', 'predata_bot_111@predata.io',
    'predata_bot_112@predata.io', 'predata_bot_113@predata.io', 'predata_bot_114@predata.io',
    'predata_bot_115@predata.io', 'predata_bot_116@predata.io', 'predata_bot_117@predata.io',
    'predata_bot_118@predata.io', 'predata_bot_119@predata.io', 'predata_bot_120@predata.io',
    'predata_bot_121@predata.io', 'predata_bot_122@predata.io', 'predata_bot_123@predata.io',
    'predata_bot_124@predata.io', 'predata_bot_125@predata.io', 'predata_bot_126@predata.io',
    'predata_bot_127@predata.io', 'predata_bot_128@predata.io', 'predata_bot_129@predata.io',
    'predata_bot_130@predata.io', 'predata_bot_131@predata.io', 'predata_bot_132@predata.io',
    'predata_bot_133@predata.io', 'predata_bot_134@predata.io', 'predata_bot_135@predata.io',
    'predata_bot_136@predata.io', 'predata_bot_137@predata.io', 'predata_bot_138@predata.io',
    'predata_bot_139@predata.io', 'predata_bot_140@predata.io', 'predata_bot_141@predata.io',
    'predata_bot_142@predata.io', 'predata_bot_143@predata.io', 'predata_bot_144@predata.io',
    'predata_bot_145@predata.io', 'predata_bot_146@predata.io', 'predata_bot_147@predata.io',
    'predata_bot_148@predata.io', 'predata_bot_149@predata.io', 'predata_bot_150@predata.io',
    'predata_bot_151@predata.io', 'predata_bot_152@predata.io', 'predata_bot_153@predata.io',
    'predata_bot_154@predata.io', 'predata_bot_155@predata.io', 'predata_bot_156@predata.io',
    'predata_bot_157@predata.io', 'predata_bot_158@predata.io', 'predata_bot_159@predata.io',
    'predata_bot_160@predata.io', 'predata_bot_161@predata.io', 'predata_bot_162@predata.io',
    'predata_bot_163@predata.io', 'predata_bot_164@predata.io', 'predata_bot_165@predata.io',
    'predata_bot_166@predata.io', 'predata_bot_167@predata.io', 'predata_bot_168@predata.io',
    'predata_bot_169@predata.io', 'predata_bot_170@predata.io', 'predata_bot_171@predata.io',
    'predata_bot_172@predata.io', 'predata_bot_173@predata.io', 'predata_bot_174@predata.io',
    'predata_bot_175@predata.io', 'predata_bot_176@predata.io', 'predata_bot_177@predata.io',
    'predata_bot_178@predata.io', 'predata_bot_179@predata.io', 'predata_bot_180@predata.io',
    'predata_bot_181@predata.io', 'predata_bot_182@predata.io', 'predata_bot_183@predata.io',
    'predata_bot_184@predata.io', 'predata_bot_185@predata.io', 'predata_bot_186@predata.io',
    'predata_bot_187@predata.io', 'predata_bot_188@predata.io', 'predata_bot_189@predata.io',
    'predata_bot_190@predata.io', 'predata_bot_191@predata.io', 'predata_bot_192@predata.io',
    'predata_bot_193@predata.io', 'predata_bot_194@predata.io', 'predata_bot_195@predata.io',
    'predata_bot_196@predata.io', 'predata_bot_197@predata.io', 'predata_bot_198@predata.io',
    'predata_bot_199@predata.io', 'predata_bot_200@predata.io', 'predata_bot_201@predata.io',
    'predata_bot_202@predata.io', 'predata_bot_203@predata.io', 'predata_bot_204@predata.io',
    'predata_bot_205@predata.io', 'predata_bot_206@predata.io', 'predata_bot_207@predata.io',
    'predata_bot_208@predata.io', 'predata_bot_209@predata.io', 'predata_bot_210@predata.io',
    'predata_bot_211@predata.io', 'predata_bot_212@predata.io', 'predata_bot_213@predata.io',
    'predata_bot_214@predata.io', 'predata_bot_215@predata.io', 'predata_bot_216@predata.io',
    'predata_bot_217@predata.io', 'predata_bot_218@predata.io', 'predata_bot_219@predata.io',
    'predata_bot_220@predata.io', 'predata_bot_221@predata.io', 'predata_bot_222@predata.io',
    'predata_bot_223@predata.io', 'predata_bot_224@predata.io', 'predata_bot_225@predata.io',
    'predata_bot_226@predata.io', 'predata_bot_227@predata.io', 'predata_bot_228@predata.io',
    'predata_bot_229@predata.io', 'predata_bot_230@predata.io', 'predata_bot_231@predata.io',
    'predata_bot_232@predata.io', 'predata_bot_233@predata.io', 'predata_bot_234@predata.io',
    'predata_bot_235@predata.io', 'predata_bot_236@predata.io', 'predata_bot_237@predata.io',
    'predata_bot_238@predata.io', 'predata_bot_239@predata.io', 'predata_bot_240@predata.io',
    'predata_bot_241@predata.io', 'predata_bot_242@predata.io', 'predata_bot_243@predata.io',
    'predata_bot_244@predata.io', 'predata_bot_245@predata.io', 'predata_bot_246@predata.io',
    'predata_bot_247@predata.io', 'predata_bot_248@predata.io', 'predata_bot_249@predata.io',
    'predata_bot_250@predata.io', 'predata_bot_251@predata.io', 'predata_bot_252@predata.io',
    'predata_bot_253@predata.io', 'predata_bot_254@predata.io', 'predata_bot_255@predata.io',
    'predata_bot_256@predata.io', 'predata_bot_257@predata.io', 'predata_bot_258@predata.io',
    'predata_bot_259@predata.io', 'predata_bot_260@predata.io', 'predata_bot_261@predata.io',
    'predata_bot_262@predata.io', 'predata_bot_263@predata.io', 'predata_bot_264@predata.io',
    'predata_bot_265@predata.io', 'predata_bot_266@predata.io', 'predata_bot_267@predata.io',
    'predata_bot_268@predata.io', 'predata_bot_269@predata.io', 'predata_bot_270@predata.io',
    'predata_bot_271@predata.io', 'predata_bot_272@predata.io', 'predata_bot_273@predata.io',
    'predata_bot_274@predata.io', 'predata_bot_275@predata.io', 'predata_bot_276@predata.io',
    'predata_bot_277@predata.io', 'predata_bot_278@predata.io', 'predata_bot_279@predata.io',
    'predata_bot_280@predata.io', 'predata_bot_281@predata.io', 'predata_bot_282@predata.io',
    'predata_bot_283@predata.io', 'predata_bot_284@predata.io', 'predata_bot_285@predata.io',
    'predata_bot_286@predata.io', 'predata_bot_287@predata.io', 'predata_bot_288@predata.io',
    'predata_bot_289@predata.io', 'predata_bot_290@predata.io', 'predata_bot_291@predata.io',
    'predata_bot_292@predata.io', 'predata_bot_293@predata.io', 'predata_bot_294@predata.io',
    'predata_bot_295@predata.io', 'predata_bot_296@predata.io', 'predata_bot_297@predata.io',
    'predata_bot_298@predata.io', 'predata_bot_299@predata.io', 'predata_bot_300@predata.io'
)
AND a.activity_type = 'VOTE'
AND DATE(a.created_at) = @today;

-- 3. vote_records 테이블에서 삭제 (vote_id는 activity_id를 참조)
DELETE FROM vote_records
WHERE vote_id IN (SELECT activity_id FROM temp_activities_to_delete);

-- 4. activities 테이블에서 삭제
DELETE FROM activities
WHERE activity_id IN (SELECT activity_id FROM temp_activities_to_delete);

-- 5. 임시 테이블 삭제
DROP TEMPORARY TABLE temp_activities_to_delete;

-- 6. 삭제된 레코드 수 확인 (선택사항)
SELECT
    '삭제 완료' as status,
    ROW_COUNT() as deleted_activities_count;

-- 트랜잭션 커밋 (실행 전 확인 후 주석 해제)
-- COMMIT;

-- 롤백하려면 (문제가 있을 경우)
-- ROLLBACK;
