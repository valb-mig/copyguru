package com.valb.copyguru.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.valb.copyguru.data.Copy
import com.valb.copyguru.data.CopyRepository
import com.valb.copyguru.data.Segment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Debug-only. Fills the database with demo segments and copies for screenshots:
 *
 *     adb shell am broadcast -n com.valb.copyguru/.debug.DemoDataReceiver
 *
 * Re-running replaces the demo segments (and their copies) instead of duplicating them.
 * Segments the user created themselves are left untouched.
 */
class DemoDataReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val result = goAsync()
        val repository = CopyRepository.from(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val existing = repository.segmentList()
                DEMO.forEach { demo ->
                    existing.filter { it.name == demo.name }.forEach { repository.deleteSegment(it) }
                }
                var position = existing.size
                DEMO.forEach { demo ->
                    val segment = Segment(name = demo.name, color = demo.color, position = position++)
                    repository.saveSegment(segment)
                    val saved = repository.segmentList().last { it.name == demo.name }
                    demo.copies.forEach { (title, content, favorite) ->
                        repository.saveCopy(
                            Copy(
                                segmentId = saved.id,
                                title = title,
                                content = content,
                                favorite = favorite
                            )
                        )
                    }
                }
                Log.i(TAG, "demo data inserted: ${DEMO.sumOf { it.copies.size }} copies")
            } catch (e: Exception) {
                Log.e(TAG, "demo seeding failed", e)
            } finally {
                result.finish()
            }
        }
    }

    private data class DemoSegment(
        val name: String,
        val color: Int,
        val copies: List<Triple<String, String, Boolean>>
    )

    private companion object {
        const val TAG = "CopyGuruDemo"

        val PURPLE = CopyRepository.SEGMENT_COLORS[0]
        val GREEN = CopyRepository.SEGMENT_COLORS[1]
        val BLUE = CopyRepository.SEGMENT_COLORS[3]

        val DEMO = listOf(
            DemoSegment(
                name = "Vendas",
                color = PURPLE,
                copies = listOf(
                    Triple(
                        "Primeiro contato",
                        "Oi [nome], tudo bem? Vi que você comentou sobre [assunto] e acho que consigo te ajudar com isso.\n\n" +
                            "Me dá 10 minutos essa semana pra eu te mostrar como resolveríamos? Tenho hoje à tarde ou amanhã de manhã.",
                        true
                    ),
                    Triple(
                        "Follow-up 48h",
                        "Oi [nome], passando aqui pra isso não morrer no esquecimento.\n\n" +
                            "Ainda faz sentido pra você? Se não for o momento me fala tranquilo, eu te procuro mais pra frente.",
                        true
                    ),
                    Triple(
                        "Objeção: está caro",
                        "Entendo. Deixa eu te perguntar uma coisa: caro em relação a quê?\n\n" +
                            "Se o retorno aparecer em [prazo], o valor continua sendo o problema ou o incômodo é o prazo? " +
                            "Dependendo da resposta eu consigo ajustar o escopo pra caber no seu orçamento.",
                        true
                    ),
                    Triple(
                        "Objeção: vou pensar",
                        "Claro, sem pressa.\n\n" +
                            "Só pra eu te ajudar melhor: o que ficou em aberto? Se for prazo, escopo ou valor, resolvo agora com você. " +
                            "Se for outra coisa, prefiro saber do que ficar te enchendo de mensagem.",
                        false
                    ),
                    Triple(
                        "Proposta enviada",
                        "Acabei de te mandar a proposta por e-mail. São 2 páginas, dá pra ler em 3 minutos.\n\n" +
                            "Qualquer dúvida me chama aqui que eu respondo na hora.",
                        false
                    ),
                    Triple(
                        "Fechamento",
                        "Fechado então.\n\n" +
                            "Vou te mandar o contrato e o link de pagamento. Assim que confirmar, já reservo seu lugar na agenda desta semana.",
                        false
                    )
                )
            ),
            DemoSegment(
                name = "Marketing",
                color = GREEN,
                copies = listOf(
                    Triple(
                        "CTA de carrossel",
                        "Salva esse post pra não perder depois.\n\n" +
                            "E se você quer isso rodando no seu negócio, comenta ORÇAMENTO aqui embaixo que eu te chamo no direct.",
                        true
                    ),
                    Triple(
                        "Bio do Instagram",
                        "Ajudo [nicho] a vender mais sem depender só de indicação.\n" +
                            "📍 [cidade] · atendimento online\n" +
                            "👇 chama no WhatsApp",
                        false
                    ),
                    Triple(
                        "Convite lista VIP",
                        "Vou abrir só 10 vagas e quem está na lista VIP entra 24h antes de todo mundo.\n\n" +
                            "Quer entrar? Responde VIP aqui que eu te adiciono.",
                        false
                    ),
                    Triple(
                        "Aviso de lançamento",
                        "Amanhã às 19h abre a turma.\n\n" +
                            "São 10 vagas e o valor de lançamento sobe depois das primeiras 48h. Quer que eu te avise na hora que abrir?",
                        false
                    ),
                    Triple(
                        "Pedido de depoimento",
                        "Posso te pedir um favor rápido?\n\n" +
                            "Grava um áudio de 30 segundos contando o que mudou depois do nosso trabalho. " +
                            "Uso pra mostrar resultado real de cliente e te marco na publicação.",
                        false
                    )
                )
            ),
            DemoSegment(
                name = "Consultoria",
                color = BLUE,
                copies = listOf(
                    Triple(
                        "Agendar diagnóstico",
                        "Antes de falar de proposta eu faço um diagnóstico de 30 minutos, sem custo, pra entender onde está o gargalo.\n\n" +
                            "Tenho [dia] às [hora] ou [dia] às [hora]. Qual funciona melhor pra você?",
                        true
                    ),
                    Triple(
                        "Confirmação de reunião",
                        "Confirmado para [dia] às [hora].\n" +
                            "Link: [link]\n\n" +
                            "Se puder, separa 40 minutos sem interrupção. Vou pedir alguns números na hora, então deixa o relatório de [período] à mão.",
                        false
                    ),
                    Triple(
                        "Resumo pós-call",
                        "Resumo do que conversamos:\n" +
                            "1. [ponto]\n" +
                            "2. [ponto]\n" +
                            "3. [ponto]\n\n" +
                            "Próximo passo: te mando a proposta até [dia]. Ficou faltando alguma coisa?",
                        false
                    ),
                    Triple(
                        "Escopo e investimento",
                        "O trabalho tem 3 entregas: diagnóstico, plano de ação e 4 encontros de acompanhamento.\n" +
                            "Prazo: [x] semanas.\n" +
                            "Investimento: R$ [valor], em até [x]x.\n\n" +
                            "Quer que eu detalhe alguma dessas partes?",
                        false
                    ),
                    Triple(
                        "Cobrança gentil",
                        "Oi [nome], tudo certo?\n\n" +
                            "A parcela de [mês] consta em aberto aqui. Provavelmente é só esquecimento. Te mando o link de novo?",
                        false
                    ),
                    Triple(
                        "Encerramento de projeto",
                        "Fechamos o ciclo.\n\n" +
                            "Te mandei o relatório final com o antes e depois de cada indicador. " +
                            "Qualquer dúvida sobre o material pode me chamar nos próximos 30 dias, sem custo.",
                        false
                    )
                )
            )
        )
    }
}
