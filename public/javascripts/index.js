$(function(){
       // html表示時の入力フォーム総数
       var defaultFormIdx = $('#input-group').children("div").length - 1

       // 追加された入力フォームの総数（デフォルトo個）
       var addedFormCount = 0

              // 入力フォームを動的に追加する【TODO】微調整
              $('#add-button').click(function(){
                     addedFormCount++
                     
                     var formIdx = defaultFormIdx + addedFormCount
                     
                            // 画面表示上の添え字は1始まりのため添え字+1(formIdx+1)している
                            $('#input-group').append(
                                   '<div class="form-group" id="station' + formIdx + '">'
                                   + '<label for="station-name[' + formIdx + ']">出発駅' + (formIdx + 1) + '</label>'
                                   + '<input type="text" class="form-control" id="station-name[' + formIdx + ']" ' + 'name="station-name[' + formIdx + ']" ' + 'placeholder="駅名">'
                                   + '</div>'
                                   );
                            updateDelButtonState()
                     });
              
              // 入力フォームを動的に削除する
              $('#del-button').click(function(){

                     var formIdx = defaultFormIdx + addedFormCount
                     
                            // 削除ボタンの活性非活性で制御しているため、addedFormIdxのチェックは行Count
                            $('#station' + formIdx).remove();
                            addedFormCount--
                            updateDelButtonState()
                     });

              // 削除ボタンの活性・非活性を切り替える
              function updateDelButtonState(){

                     // フォームの総数が2（添え字換算で1）なら削除ボタンを押せないように制御
                     if ((defaultFormIdx + addedFormCount) === 1) {
                            $(function(){
                                   $('#del-button').prop("disabled", true);
                            });                        
                     }else{
                            $(function(){
                                   $('#del-button').prop("disabled", false);
                            });
                     }
              };
       });